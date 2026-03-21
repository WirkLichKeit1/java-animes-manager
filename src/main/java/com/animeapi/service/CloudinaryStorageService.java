package com.animeapi.service;

import com.animeapi.exception.VideoProcessingException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Slf4j
public class CloudinaryStorageService implements StorageService {

    private static final String FOLDER_PREFIX = "darkjam";

    /**
     * Threshold acima do qual usa chunked upload (100 MB).
     * O Cloudinary aceita chunks de até 100 MB por request,
     * contornando limites de nginx/proxies intermediários.
     */
    private static final long CHUNKED_UPLOAD_THRESHOLD = 100L * 1024 * 1024;

    /**
     * Tamanho de cada chunk: 95 MB (margem de segurança abaixo do limite de 100 MB).
     */
    private static final long CHUNK_SIZE = 95L * 1024 * 1024;

    private final Cloudinary cloudinary;

    public CloudinaryStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String store(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        String publicId = FOLDER_PREFIX + "/" + directory + "/" + UUID.randomUUID();
        String resourceType = directory.equals("videos") ? "video" : "image";

        Path tempFile = null;
        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = (originalFilename != null && originalFilename.contains("."))
                    ? "." + originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                    : ".tmp";

            tempFile = Files.createTempFile("upload-", suffix);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return uploadFile(tempFile.toFile(), publicId, resourceType, originalFilename);

        } catch (IOException e) {
            throw new VideoProcessingException(
                    "Failed to store file in Cloudinary: " + file.getOriginalFilename(), e);
        } finally {
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
            }
        }
    }

    @Override
    public String storeFromPath(Path tempFile, String directory, String originalFilename) {
        String publicId = FOLDER_PREFIX + "/" + directory + "/" + UUID.randomUUID();
        String resourceType = directory.equals("videos") ? "video" : "image";

        try {
            return uploadFile(tempFile.toFile(), publicId, resourceType, originalFilename);
        } catch (IOException e) {
            throw new VideoProcessingException(
                    "Failed to store file from path in Cloudinary: " + originalFilename, e);
        }
    }

    /**
     * Decide entre upload normal e chunked com base no tamanho do arquivo.
     *
     * Arquivos <= 100 MB → upload normal (único POST).
     * Arquivos > 100 MB  → chunked upload (múltiplos POSTs de 95 MB).
     *
     * O chunked upload contorna o 413 do nginx porque cada request individual
     * nunca ultrapassa o CHUNK_SIZE — o Cloudinary remonta o arquivo no final.
     */
    private String uploadFile(java.io.File file, String publicId, String resourceType, String originalFilename)
            throws IOException {

        long fileSize = file.length();

        Map<String, Object> uploadParams = new TreeMap<>();
        uploadParams.put("public_id", publicId);
        uploadParams.put("resource_type", resourceType);
        uploadParams.put("overwrite", false);

        Map<?, ?> result;

        if (fileSize > CHUNKED_UPLOAD_THRESHOLD) {
            log.info("Using chunked upload for '{}' ({} MB, {} chunks of {} MB)",
                    originalFilename,
                    String.format("%.1f", fileSize / 1_048_576.0),
                    (int) Math.ceil((double) fileSize / CHUNK_SIZE),
                    String.format("%.0f", CHUNK_SIZE / 1_048_576.0));

            uploadParams.put("chunk_size", CHUNK_SIZE);
            result = cloudinary.uploader().uploadLarge(file, uploadParams);
        } else {
            result = cloudinary.uploader().upload(file, uploadParams);
        }

        String storedKey = result.get("public_id").toString();
        log.info("Upload complete — Cloudinary key: {}", storedKey);
        return storedKey;
    }

    @Override
    public String getUrl(String publicId) {
        String resourceType = publicId.contains("/videos/") ? "video" : "image";
        return cloudinary.url().resourceType(resourceType).generate(publicId);
    }

    @Override
    public InputStream load(String publicId) {
        try {
            String url = getUrl(publicId);
            return URI.create(url).toURL().openStream();
        } catch (IOException e) {
            throw new VideoProcessingException("Failed to load file from Cloudinary: " + publicId, e);
        }
    }

    @Override
    public long getFileSize(String publicId) {
        try {
            String resourceType = publicId.contains("/videos/") ? "video" : "image";
            Map<?, ?> result = cloudinary.api()
                    .resource(publicId, ObjectUtils.asMap("resource_type", resourceType));
            Object bytes = result.get("bytes");
            if (bytes == null) {
                throw new VideoProcessingException("Could not determine file size for: " + publicId);
            }
            return Long.parseLong(bytes.toString());
        } catch (Exception e) {
            throw new VideoProcessingException("Failed to get file size from Cloudinary: " + publicId, e);
        }
    }

    @Override
    public void delete(String publicId) {
        try {
            String resourceType = publicId.contains("/videos/") ? "video" : "image";
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", resourceType)
            );
            log.info("Deleted file from Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.warn("Failed to delete file from Cloudinary: {}", publicId, e);
        }
    }

    @Override
    public boolean exists(String publicId) {
        try {
            String resourceType = publicId.contains("/videos/") ? "video" : "image";
            cloudinary.api().resource(publicId, ObjectUtils.asMap("resource_type", resourceType));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}