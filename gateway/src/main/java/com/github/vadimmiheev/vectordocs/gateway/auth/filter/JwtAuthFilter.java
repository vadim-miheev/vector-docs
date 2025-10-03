package com.github.vadimmiheev.vectordocs.gateway.auth.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.vadimmiheev.vectordocs.gateway.auth.service.JwtService;
import lombok.AllArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@AllArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Allow non-API paths
        if (path == null || !path.startsWith("/api/")) {
            return chain.filter(exchange);
        }

        // Allow CORS preflight
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // Allow authentication endpoints
        if (path.startsWith("/api/auth")) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7).trim();
        try {
            DecodedJWT jwt = jwtService.verifyToken(token);
            Long uid = jwt.getClaim("uid").asLong();
            if (uid == null) {
                return unauthorized(exchange, "Token missing uid claim");
            }

            ServerHttpRequest mutated = request.mutate()
                    .headers(httpHeaders -> {
                        httpHeaders.set("X-User-Id", String.valueOf(uid));
                    })
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception ex) {
            return unauthorized(exchange, "Invalid token");
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"error\":\"unauthorized\",\"message\":\"" + message.replace("\"", "\\\"") + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -1; // run before most filters
    }
}
