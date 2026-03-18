package com.animeapi.service;

import com.animeapi.exception.VideoProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
public class LocalStorageService implements StorageService {

    private final Path basePath;

    public LocalStorageService(String basePath) {
        this.basePath = Paths.get(basePath);
        initDirectories();
    }

    private void initDirectories() {
        try {
            Files.createDirectories(basePath.resolve("videos"));
            Files.createDirectories(basePath.resolve("images"));
            log.info("Storage directories initialized at: {}", basePath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directories", e);
        }
    }

    @Override
    public String store(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String filename = directory + "/" + UUID.randomUUID() + "." + extension;

        try {
            Path destination = basePath.resolve(filename);
            Files.createDirectories(destination.getParent());
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: {}", filename);
            return filename;
        } catch (IOException e) {
            throw new VideoProcessingException("Failed to store file: " + originalFilename, e);
        }
    }

    @Override
    public InputStream load(String filename) {
        try {
            Path file = basePath.resolve(filename);
            if (!Files.exists(file)) {
                throw new VideoProcessingException("File not found: " + filename);
            }
            return Files.newInputStream(file);
        } catch (IOException e) {
            throw new VideoProcessingException("Failed to load file: " + filename, e);
        }
    }

    @Override
    public long getFileSize(String filename) {
        try {
            return Files.size(basePath.resolve(filename));
        } catch (IOException e) {
            throw new VideoProcessingException("Failed to get file size: " + filename, e);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            Path file = basePath.resolve(filename);
            Files.deleteIfExists(file);
            log.info("Deleted file: {}", filename);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", filename, e);
        }
    }

    @Override
    public boolean exists(String filename) {
        return Files.exists(basePath.resolve(filename));
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}