package com.animeapi.service;

import com.animeapi.exception.VideoProcessingException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Slf4j
public class CloudinaryStorageService implements StorageService {

    private static final String FOLDER_PREFIX = "darkjam";

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

        try {
            // TreeMap garante ordem alfabética dos parâmetros,
            // necessária para a assinatura HMAC do Cloudinary.
            Map<String, Object> uploadParams = new TreeMap<>();
            uploadParams.put("overwrite", false);
            uploadParams.put("public_id", publicId);
            uploadParams.put("resource_type", resourceType);

            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            String storedKey = result.get("public_id").toString();
            log.info("Stored file in Cloudinary: {}", storedKey);
            return storedKey;

        } catch (IOException e) {
            throw new VideoProcessingException("Failed to store file in Cloudinary: " + file.getOriginalFilename(), e);
        }
    }

    /**
     * Retorna a URL pública CDN do Cloudinary para o arquivo.
     * Esta URL é usada diretamente no frontend para carregar imagens.
     */
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