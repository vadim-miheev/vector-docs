package com.github.vadimmiheev.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoutesConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("speech_to_text", r -> r
                        .path("/api/speech/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://speech-to-text:8080"))
                .route("ai_interpreter", r -> r
                        .path("/api/ai/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://ai-interpreter"))
                .route("common_service", r -> r
                        .path("/api/common/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://common"))
                .build();
    }
}

