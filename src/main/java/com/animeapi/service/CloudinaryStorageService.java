package com.animeapi.service;

import com.animeapi.dto.response.VideoUploadSignatureResponse;
import com.animeapi.exception.VideoProcessingException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Slf4j
public class CloudinaryStorageService implements StorageService {

    private static final String FOLDER_PREFIX = "darkjam";
    private static final long CHUNKED_UPLOAD_THRESHOLD = 100L * 1024 * 1024; // 100 MB
    private static final long CHUNK_SIZE = 95L * 1024 * 1024;                // 95 MB

    private final Cloudinary cloudinary;
    private final String apiSecret;
    private final String apiKey;
    private final String cloudName;

    public CloudinaryStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
        this.apiSecret = cloudinary.config.apiSecret;
        this.apiKey = cloudinary.config.apiKey;
        this.cloudName = cloudinary.config.cloudName;
    }

    /**
     * Gera uma assinatura HMAC-SHA256 para upload direto pelo frontend.
     *
     * O Cloudinary valida a assinatura para garantir que o upload foi autorizado
     * pelo backend. O api_secret nunca é exposto ao frontend — apenas a assinatura.
     *
     * Parâmetros assinados: public_id + timestamp (em ordem alfabética).
     * Formato: SHA256(public_id=xxx&timestamp=yyy + api_secret)
     */
    @Override
    public VideoUploadSignatureResponse generateUploadSignature(String directory) {
        try {
            String publicId = FOLDER_PREFIX + "/" + directory + "/" + UUID.randomUUID();
            long timestamp = System.currentTimeMillis() / 1000L;

            // Parâmetros em ordem alfabética — obrigatório para a assinatura do Cloudinary
            String paramsToSign = "public_id=" + publicId + "&timestamp=" + timestamp;
            String signature = hmacSha256(paramsToSign + apiSecret);

            log.debug("Upload signature generated for publicId: {}", publicId);
            return new VideoUploadSignatureResponse(signature, timestamp, apiKey, cloudName, publicId);

        } catch (Exception e) {
            throw new VideoProcessingException("Failed to generate upload signature", e);
        }
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
            String suffix = getSuffix(originalFilename);
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
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
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

    private String hmacSha256(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(apiSecret.getBytes(), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes()));
    }

    private String getSuffix(String filename) {
        if (filename != null && filename.contains(".")) {
            return "." + filename.substring(filename.lastIndexOf('.') + 1);
        }
        return ".tmp";
    }
}