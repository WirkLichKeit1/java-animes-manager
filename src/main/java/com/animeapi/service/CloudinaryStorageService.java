package com.animeapi.service;

import com.animeapi.exception.VideoProcessingException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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

        // *** CORRIGIDO: não usa mais file.getBytes() ***
        // file.getBytes() carrega o arquivo inteiro na heap — com arquivos grandes
        // (ex: vídeos de 280 MB) isso causa OutOfMemoryError e derruba a aplicação.
        //
        // Solução: grava o MultipartFile em um arquivo temporário no disco e faz
        // upload a partir dele. O Cloudinary SDK lê o arquivo via stream internamente,
        // sem carregá-lo inteiramente na memória.
        //
        // O arquivo temporário é deletado no bloco finally, independente do resultado.
        Path tempFile = null;
        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = (originalFilename != null && originalFilename.contains("."))
                    ? "." + originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                    : ".tmp";

            tempFile = Files.createTempFile("upload-", suffix);

            // Copia o stream do MultipartFile para o arquivo temporário
            // sem passar pela heap (copia diretamente entre streams)
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            Map<String, Object> uploadParams = new TreeMap<>();
            uploadParams.put("overwrite", false);
            uploadParams.put("public_id", publicId);
            uploadParams.put("resource_type", resourceType);

            // Upload via File — o SDK faz chunked upload internamente para vídeos grandes
            Map<?, ?> result = cloudinary.uploader().upload(tempFile.toFile(), uploadParams);

            String storedKey = result.get("public_id").toString();
            log.info("Stored file in Cloudinary: {}", storedKey);
            return storedKey;

        } catch (IOException e) {
            throw new VideoProcessingException(
                    "Failed to store file in Cloudinary: " + file.getOriginalFilename(), e);
        } finally {
            // Garante que o arquivo temporário seja deletado mesmo em caso de erro
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temp file: {}", tempFile, e);
                }
            }
        }
    }

    /**
     * Upload a partir de um arquivo temporário no disco.
     * Usado pelo VideoService para fazer upload assíncrono sem carregar o
     * arquivo inteiro na heap.
     */
    @Override
    public String storeFromPath(Path tempFile, String directory, String originalFilename) {
        String publicId = FOLDER_PREFIX + "/" + directory + "/" + UUID.randomUUID();
        String resourceType = directory.equals("videos") ? "video" : "image";

        try {
            Map<String, Object> uploadParams = new TreeMap<>();
            uploadParams.put("overwrite", false);
            uploadParams.put("public_id", publicId);
            uploadParams.put("resource_type", resourceType);

            Map<?, ?> result = cloudinary.uploader().upload(tempFile.toFile(), uploadParams);

            String storedKey = result.get("public_id").toString();
            log.info("Stored file from path in Cloudinary: {}", storedKey);
            return storedKey;

        } catch (IOException e) {
            throw new VideoProcessingException(
                    "Failed to store file from path in Cloudinary: " + originalFilename, e);
        }
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