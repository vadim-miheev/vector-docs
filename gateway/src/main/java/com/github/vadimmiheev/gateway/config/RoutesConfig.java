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
                .route("search_service", r -> r
                        .path("/api/search/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://search-service:8080"))
                .route("document_processor", r -> r
                        .path("/api/docs/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://document-processor:8080"))
                .build();
    }
}

