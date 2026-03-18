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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private static final List<String> ALLOWED_FORMATS = Arrays.asList("mp4", "mkv", "avi", "webm");
    private static final long CHUNK_SIZE = 1024 * 1024; // 1MB por chunk

    private final EpisodeRepository episodeRepository;
    private final StorageService storageService;

    @Transactional
    public void uploadVideo(Long episodeId, MultipartFile file) {
        Episode episode = getEpisodeOrThrow(episodeId);

        validateVideoFile(file);

        // Remove vídeo antigo se existir
        if (episode.getVideoFilename() != null) {
            storageService.delete(episode.getVideoFilename());
        }

        episode.setVideoStatus(VideoStatus.PROCESSING);
        episodeRepository.save(episode);

        String filename = storageService.store(file, "videos");
        processVideoAsync(episodeId, filename);
    }

    @Async
    @Transactional
    public void processVideoAsync(Long episodeId, String filename) {
        try {
            // Simula processamento (em produção: ffmpeg para transcodificação)
            Thread.sleep(1000);

            Episode episode = getEpisodeOrThrow(episodeId);
            episode.setVideoFilename(filename);
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

    public ResponseEntity<byte[]> streamVideo(Long episodeId, String rangeHeader) {
        Episode episode = getEpisodeOrThrow(episodeId);

        if (episode.getVideoStatus() != VideoStatus.READY) {
            throw new VideoProcessingException("Video is not ready for streaming");
        }

        String filename = episode.getVideoFilename();
        long fileSize = storageService.getFileSize(filename);
        String contentType = resolveContentType(filename);

        // Sem Range header — retorna arquivo completo
        if (rangeHeader == null) {
            try (InputStream is = storageService.load(filename)) {
                byte[] data = is.readAllBytes();
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_TYPE, contentType);
                headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize));
                headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
                return new ResponseEntity<>(data, headers, HttpStatus.OK);
            } catch (IOException e) {
                throw new VideoProcessingException("Failed to stream video", e);
            }
        }

        // Com Range header — retorna chunk parcial (HTTP 206)
        long[] range = parseRange(rangeHeader, fileSize);
        long start = range[0];
        long end = range[1];
        long contentLength = end - start + 1;

        try (InputStream is = storageService.load(filename)) {
            is.skip(start);
            byte[] data = is.readNBytes((int) contentLength);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, contentType);
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
            headers.add(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
            headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");

            return new ResponseEntity<>(data, headers, HttpStatus.PARTIAL_CONTENT);
        } catch (IOException e) {
            throw new VideoProcessingException("Failed to stream video chunk", e);
        }
    }

    private long[] parseRange(String rangeHeader, long fileSize) {
        // Range: bytes=0-1048576
        String range = rangeHeader.replace("bytes=", "");
        String[] parts = range.split("-");

        long start = Long.parseLong(parts[0]);
        long end = parts.length > 1 && !parts[1].isEmpty()
                ? Long.parseLong(parts[1])
                : Math.min(start + CHUNK_SIZE - 1, fileSize - 1);

        if (start >= fileSize || end >= fileSize) {
            throw new VideoProcessingException("Invalid range: " + rangeHeader);
        }

        return new long[]{start, end};
    }

    private void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Video file must not be empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_FORMATS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Invalid video format. Allowed: " + String.join(", ", ALLOWED_FORMATS));
        }
    }

    private String resolveContentType(String filename) {
        if (filename.endsWith(".mp4")) return "video/mp4";
        if (filename.endsWith(".webm")) return "video/webm";
        if (filename.endsWith(".mkv")) return "video/x-matroska";
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