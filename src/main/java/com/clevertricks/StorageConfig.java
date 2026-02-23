package com.clevertricks;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {
    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "filesystem", matchIfMissing = true)
    public StorageService fileSystemStorageService(StorageProperties properties) {
        return new FileSystemStorageService(properties);
    }

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "postgresql")
    public StorageService postgresqlStorageService(StorageProperties properties) {
        return new PostgresqlStorageService(properties);
    }
}
