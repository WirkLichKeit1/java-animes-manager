package com.animeapi.service;

import com.animeapi.exception.ResourceNotFoundException;
import com.animeapi.exception.VideoProcessingException;
import com.animeapi.model.Episode;
import com.animeapi.model.VideoStatus;
import com.animeapi.repository.EpisodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private static final List<String> ALLOWED_FORMATS = Arrays.asList("mp4", "mkv", "avi", "webm");
    private static final long CHUNK_SIZE = 2 * 1024 * 1024; // 2MB
    private static final long MAX_CHUNK = (long) Integer.MAX_VALUE;

    private final EpisodeRepository episodeRepository;
    private final StorageService storageService;
    private final VideoStatusUpdater videoStatusUpdater;

    /**
     * Recebe o vídeo, grava em disco temporariamente e responde ao cliente
     * imediatamente com status PROCESSING. O upload para o Cloudinary acontece
     * em background via @Async.
     *
     * Isso resolve dois problemas:
     * 1. O request HTTP não fica bloqueado durante o upload de centenas de MB,
     *    evitando timeout no frontend e reinício da aplicação no Render.
     * 2. O MultipartFile precisa ser copiado para disco ANTES do request terminar —
     *    depois que o request fecha, o servlet invalida o InputStream do MultipartFile.
     */
    @Transactional
    public void uploadVideo(Long episodeId, MultipartFile file) {
        Episode episode = getEpisodeOrThrow(episodeId);
        validateVideoFile(file);

        if (episode.getVideoFilename() != null) {
            storageService.delete(episode.getVideoFilename());
            episode.setVideoFilename(null);
        }

        // Copia para disco dentro do request — depois que o request fechar,
        // o InputStream do MultipartFile fica inválido.
        Path tempFile = copyToTempFile(file);

        episode.setVideoStatus(VideoStatus.PROCESSING);
        episodeRepository.save(episode);

        log.info("Video received for episode {}, starting async upload ({} MB)",
                episodeId, String.format("%.1f", tempFile.toFile().length() / 1_048_576.0));

        uploadToStorageAsync(episodeId, tempFile, file.getOriginalFilename());
    }

    @Async
    public void uploadToStorageAsync(Long episodeId, Path tempFile, String originalFilename) {
        try {
            Optional<Episode> ep = episodeRepository.findById(episodeId);
            if (ep.isEmpty()) {
                log.warn("Episode {} deleted before async upload started — aborting.", episodeId);
                return;
            }

            log.info("Uploading to Cloudinary for episode {}: {}", episodeId, originalFilename);

            String filename = storageService.storeFromPath(tempFile, "videos", originalFilename);

            // Checa novamente — pode ter sido deletado durante o upload (que pode demorar minutos)
            Optional<Episode> epAfterUpload = episodeRepository.findById(episodeId);
            if (epAfterUpload.isEmpty()) {
                log.warn("Episode {} deleted during upload — cleaning up orphan file '{}'.",
                         episodeId, filename);
                storageService.delete(filename);
                return;
            }

            videoStatusUpdater.markReadyWithFilename(episodeId, filename);
            log.info("Upload complete for episode {}: {}", episodeId, filename);

        } catch (Exception e) {
            log.error("Async upload failed for episode {}: {}", episodeId, e.getMessage(), e);
            videoStatusUpdater.markError(episodeId);
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", tempFile, e);
            }
        }
    }

    @Transactional
    public ResponseEntity<StreamingResponseBody> streamVideo(Long episodeId, String rangeHeader) {
        Episode episode = getEpisodeOrThrow(episodeId);

        if (episode.getVideoStatus() != VideoStatus.READY) {
            throw new VideoProcessingException("Video is not ready for streaming");
        }

        episodeRepository.incrementViews(episodeId);

        String filename = episode.getVideoFilename();
        long fileSize = storageService.getFileSize(filename);
        String contentType = resolveContentType(filename);

        if (rangeHeader == null) {
            StreamingResponseBody body = outputStream -> {
                try (InputStream is = storageService.load(filename)) {
                    is.transferTo(outputStream);
                } catch (IOException e) {
                    throw new VideoProcessingException("Failed to stream video", e);
                }
            };
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(body);
        }

        long[] range = parseRange(rangeHeader, fileSize);
        long start = range[0];
        long end = range[1];
        long contentLength = end - start + 1;

        if (contentLength > MAX_CHUNK) {
            end = start + MAX_CHUNK - 1;
            contentLength = MAX_CHUNK;
        }

        final long finalStart = start;
        final long finalContentLength = contentLength;
        final long finalEnd = end;

        StreamingResponseBody body = outputStream -> {
            try (InputStream is = storageService.load(filename)) {
                is.skip(finalStart);
                byte[] buffer = new byte[8192];
                long remaining = finalContentLength;
                int read;
                while (remaining > 0 &&
                        (read = is.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    outputStream.write(buffer, 0, read);
                    remaining -= read;
                }
            } catch (IOException e) {
                throw new VideoProcessingException("Failed to stream video chunk", e);
            }
        };

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(finalContentLength))
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + finalEnd + "/" + fileSize)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(body);
    }

    private Path copyToTempFile(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = (originalFilename != null && originalFilename.contains("."))
                    ? "." + originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                    : ".tmp";
            Path tempFile = Files.createTempFile("video-upload-", suffix);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return tempFile;
        } catch (IOException e) {
            throw new VideoProcessingException("Failed to copy upload to temp file", e);
        }
    }

    private long[] parseRange(String rangeHeader, long fileSize) {
        String range = rangeHeader.replace("bytes=", "");
        String[] parts = range.split("-");
        long start = Long.parseLong(parts[0].trim());
        long end = (parts.length > 1 && !parts[1].trim().isEmpty())
                ? Long.parseLong(parts[1].trim())
                : Math.min(start + CHUNK_SIZE - 1, fileSize - 1);

        if (start < 0 || start >= fileSize || end >= fileSize || start > end) {
            throw new VideoProcessingException("Invalid range: " + rangeHeader);
        }
        return new long[]{start, end};
    }

    private void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Video file must not be empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("Invalid filename");
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_FORMATS.contains(extension)) {
            throw new IllegalArgumentException("Invalid video format. Allowed: " + String.join(", ", ALLOWED_FORMATS));
        }
    }

    private String resolveContentType(String filename) {
        if (filename.endsWith(".mp4"))  return "video/mp4";
        if (filename.endsWith(".webm")) return "video/webm";
        if (filename.endsWith(".mkv"))  return "video/x-matroska";
        return "video/mp4";
    }

    private Episode getEpisodeOrThrow(Long episodeId) {
        return episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Episode", episodeId));
    }
}