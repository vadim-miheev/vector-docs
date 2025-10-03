package com.github.vadimmiheev.vectordocs.gateway.config;

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

    // TODO Only for Dev
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }

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
                        .path("/api/notifications/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://notification-service:8080"))
                .build();
    }
}

