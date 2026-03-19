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
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private static final List<String> ALLOWED_FORMATS = Arrays.asList("mp4", "mkv", "avi", "webm");
    private static final long CHUNK_SIZE = 2 * 1024 * 1024;
    private static final long MAX_CHUNK = (long) Integer.MAX_VALUE;

    private final EpisodeRepository episodeRepository;
    private final StorageService storageService;

    @Transactional
    public void uploadVideo(Long episodeId, MultipartFile file) {
        Episode episode = getEpisodeOrThrow(episodeId);
        validateVideoFile(file);

        if (episode.getVideoFilename() != null) {
            storageService.delete(episode.getVideoFilename());
        }

        String filename = storageService.store(file, "videos");
        episode.setVideoFilename(filename);
        episode.setVideoStatus(VideoStatus.PROCESSING);
        episodeRepository.save(episode);

        processVideoAsync(episodeId, filename);
    }

    @Async
    public void processVideoAsync(Long episodeId, String filename) {
        updateEpisodeStatus(episodeId, filename);
    }

    @Transactional
    public void updateEpisodeStatus(Long episodeId, String filename) {
        try {
            Thread.sleep(1000);
            Episode episode = getEpisodeOrThrow(episodeId);
            episode.setVideoStatus(VideoStatus.READY);
            episodeRepository.save(episode);
            log.info("Video processed successfully for episode {}: {}", episodeId, filename);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markEpisodeAsError(episodeId);
        } catch (Exception e) {
            log.error("Error processing video for episode {}: {}", episodeId, e.getMessage(), e);
            markEpisodeAsError(episodeId);
        }
    }

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
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Video file must not be empty");
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) throw new IllegalArgumentException("Invalid filename");
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_FORMATS.contains(extension))
            throw new IllegalArgumentException("Invalid video format. Allowed: " + String.join(", ", ALLOWED_FORMATS));
    }

    private String resolveContentType(String filename) {
        if (filename.endsWith(".mp4"))  return "video/mp4";
        if (filename.endsWith(".webm")) return "video/webm";
        if (filename.endsWith(".mkv"))  return "video/x-matroska";
        return "video/mp4";
    }

    private void markEpisodeAsError(Long episodeId) {
        try {
            Episode episode = getEpisodeOrThrow(episodeId);
            episode.setVideoStatus(VideoStatus.ERROR);
            episodeRepository.save(episode);
        } catch (Exception e) {
            log.error("Failed to mark episode {} as error", episodeId, e);
        }
    }

    private Episode getEpisodeOrThrow(Long episodeId) {
        return episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Episode", episodeId));
    }
}