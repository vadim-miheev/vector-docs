package com.github.vadimmiheev.vectordocs.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class RoutesConfig {

    @Value("${app.ui.host}")
    private String uiHost;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("search_service", r -> r
                        .path("/api/search/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://search-service:8080"))
                .route("document_processor", r -> r
                        .path("/api/docs/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://document-processor:8080"))
                .route("storage_service", r -> r
                        .path("/api/storage/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://storage-service:8080"))
                .route("notification_service", r -> r
                        .path("/api/ws/notifications/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("ws://notification-service:8080"))
                .route("ui", r -> r
                        .path("/**")
                        .uri(uiHost))
                .build();
    }
}

