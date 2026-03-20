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

        // *** CORRIGIDO: valida extensão antes de usar ***
        // Impede path traversal via extensão maliciosa (ex: "../../etc/passwd")
        if (extension.contains("/") || extension.contains("\\") || extension.contains("..") || extension.isBlank()) {
            throw new IllegalArgumentException("Invalid file extension: " + extension);
        }

        String filename = directory + "/" + UUID.randomUUID() + "." + extension;

        // *** CORRIGIDO: normaliza e valida que o destino está dentro do basePath ***
        Path destination = basePath.resolve(filename).normalize();

        if (!destination.startsWith(basePath)) {
            throw new IllegalArgumentException("Path traversal attempt detected for file: " + originalFilename);
        }

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
    public InputStream load(String filename) {
        // *** CORRIGIDO: valida path também no load ***
        Path file = basePath.resolve(filename).normalize();

        if (!file.startsWith(basePath)) {
            throw new IllegalArgumentException("Path traversal attempt detected for: " + filename);
        }

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
        Path file = basePath.resolve(filename).normalize();

        if (!file.startsWith(basePath)) {
            throw new IllegalArgumentException("Path traversal attempt detected for: " + filename);
        }

        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new VideoProcessingException("Failed to get file size: " + filename, e);
        }
    }

    @Override
    public void delete(String filename) {
        Path file = basePath.resolve(filename).normalize();

        if (!file.startsWith(basePath)) {
            log.warn("Path traversal attempt on delete for: {}", filename);
            return;
        }

        try {
            Files.deleteIfExists(file);
            log.info("Deleted file: {}", filename);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", filename, e);
        }
    }

    @Override
    public boolean exists(String filename) {
        Path file = basePath.resolve(filename).normalize();

        if (!file.startsWith(basePath)) {
            return false;
        }

        return Files.exists(file);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        // Pega apenas a última extensão e remove qualquer caracter suspeito
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        // Aceita apenas caracteres alfanuméricos na extensão
        if (!ext.matches("[a-z0-9]+")) {
            return "bin";
        }
        return ext;
    }
}