package com.clevertricks;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {
    @Bean
    public StorageService posstgresqFileSystemService(StorageProperties properties) {
        return new PostgresqlFileSystemStorageService(properties);
    }
}
