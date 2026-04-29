package com.daniphord.mahanga.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class StorageDirectoryConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageDirectoryConfig.class);

    @Bean
    ApplicationRunner ensureStorageDirectories(
            @Value("${froms.storage.videos-dir:videos}") String videosDirectory,
            @Value("${froms.storage.investigations-dir:investigations}") String investigationsDirectory,
            @Value("${froms.storage.login-carousel-dir:login-carousel}") String loginCarouselDirectory
    ) {
        return args -> {
            createDirectory(videosDirectory);
            createDirectory(investigationsDirectory);
            createDirectory(loginCarouselDirectory);
        };
    }

    private void createDirectory(String directory) throws Exception {
        Path path = Path.of(directory).toAbsolutePath().normalize();
        Files.createDirectories(path);
        log.info("Storage directory ready: {}", path);
    }
}
