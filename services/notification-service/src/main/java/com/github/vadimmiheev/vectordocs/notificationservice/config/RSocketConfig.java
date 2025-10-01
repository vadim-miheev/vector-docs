package com.github.vadimmiheev.vectordocs.notificationservice.config;

import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.MimeType;

import java.util.Map;

@Configuration
public class RSocketConfig {

    @Bean
    public RSocketStrategiesCustomizer jsonMetadataExtractor() {
        // Register extractor that decodes application/json composite metadata entry into a Map under header name "metadata"
        return strategies -> strategies.metadataExtractorRegistry(registry ->
            registry.metadataToExtract(MimeType.valueOf("application/json"), Map.class, "metadata")
        );
    }
}
