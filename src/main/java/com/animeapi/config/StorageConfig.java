package com.animeapi.config;

import com.animeapi.service.CloudinaryStorageService;
import com.animeapi.service.LocalStorageService;
import com.animeapi.service.StorageService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class StorageConfig {

    @Value("${app.storage.type:local}")
    private String storageType;

    // Local storage
    @Value("${app.storage.local.base-path:./uploads}")
    private String localBasePath;

    // Cloudinary
    @Value("${app.storage.cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${app.storage.cloudinary.api-key:}")
    private String apiKey;

    @Value("${app.storage.cloudinary.api-secret:}")
    private String apiSecret;

    @Bean
    public StorageService storageService() {
        if ("cloudinary".equalsIgnoreCase(storageType)) {
            log.info("Using Cloudinary storage — cloud: {}", cloudName);
            return new CloudinaryStorageService(buildCloudinary());
        }

        log.info("Using LOCAL storage at: {}", localBasePath);
        return new LocalStorageService(localBasePath);
    }

    private Cloudinary buildCloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }
}