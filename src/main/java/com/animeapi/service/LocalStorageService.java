package com.animeapi.service;

import com.animeapi.dto.response.VideoUploadSignatureResponse;
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
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
        initDirectories();
    }

    private void initDirectories() {
        try {
            Files.createDirectories(this.basePath.resolve("videos"));
            Files.createDirectories(this.basePath.resolve("images"));
            log.info("Storage directories initialized at: {}", this.basePath);
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
        Path destination = resolveSafe(filename);

        try {
            Files.createDirectories(destination.getParent());
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: {}", filename);
            return filename;
        } catch (IOException e) {
            throw new VideoProcessingException("Failed to store file: " + originalFilename, e);
        }
    }

    @Override
    public String storeFromPath(Path tempFile, String directory, String originalFilename) {
        String extension = getExtension(originalFilename);
        String filename = directory + "/" + UUID.randomUUID() + "." + extension;
        Path destination = resolveSafe(filename);

        try {
            Files.createDirectories(destination.getParent());
            Files.copy(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file from temp: {}", filename);
            return filename;
        } catch (IOException e) {
            throw new VideoProcessingException("Failed to store file from temp: " + originalFilename, e);
        }
    }

    /**
     * Upload direto não é suportado em armazenamento local.
     * Em desenvolvimento, use o endpoint de upload multipart normal ou
     * configure STORAGE_TYPE=cloudinary.
     */
    @Override
    public VideoUploadSignatureResponse generateUploadSignature(String directory) {
        throw new UnsupportedOperationException(
                "Direct upload signatures are only supported with Cloudinary storage. " +
                "Set STORAGE_TYPE=cloudinary or use the multipart upload endpoint instead.");
    }

    @Override
    public String getUrl(String key) {
        return key;
    }

    @Override
    public InputStream load(String filename) {
        Path file = resolveSafe(filename);
        try {
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
        Path file = resolveSafe(filename);
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new VideoProcessingException("Failed to get file size: " + filename, e);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            Path file = resolveSafe(filename);
            Files.deleteIfExists(file);
            log.info("Deleted file: {}", filename);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", filename, e);
        }
    }

    @Override
    public boolean exists(String filename) {
        try {
            return Files.exists(resolveSafe(filename));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Path resolveSafe(String filename) {
        Path resolved = basePath.resolve(filename).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new IllegalArgumentException("Path traversal attempt detected for: " + filename);
        }
        return resolved;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return ext.matches("[a-z0-9]+") ? ext : "bin";
    }
}