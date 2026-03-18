package com.animeapi.config;

import com.animeapi.service.LocalStorageService;
import com.animeapi.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class StorageConfig {
    @Value("${app.storage.type:local}")
    private String storageType;

    @Value("${app.storage.local.base-path:./uploads}")
    private String localBasePath;

    @Bean
    public StorageService storageService() {
        if ("s3".equalsIgnoreCase(storageType)) {
            // Para user S3z adicione a dependência aws-sdk e implemente S3StorageService
            throw new IllegalStateException(
                "S3 storage not yet configured. Set STORAGE_TYPE=local or implement S3StorageService."
            );
        }

        log.info("Using LOCAL storage at: {}", localBasePath);
        return new LocalStorageService(localBasePath);
    }
}