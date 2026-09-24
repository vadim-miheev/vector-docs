package com.github.vadimmiheev.vectordocs.answergenerator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationClient {

    private final RSocketRequester.Builder rSocketRequesterBuilder;
    private final ObjectMapper objectMapper;


    @Value("${app.notification.rsocket.host:notification-service}")
    private String rsocketHost;

    @Value("${app.notification.rsocket.port:7000}")
    private int rsocketPort;

    private volatile RSocketRequester requester;

    private RSocketRequester getOrCreateRequester() {
        if (requester == null) {
            synchronized (this) {
                if (requester == null) {
                    requester = rSocketRequesterBuilder
                        .rsocketConnector(connector -> connector
                            .reconnect(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(5)))
                        )
                        .tcp(rsocketHost, rsocketPort);
                    log.info("Created RSocket requester for {}:{}", rsocketHost, rsocketPort);
                }
            }
        }
        return requester;
    }

    @PreDestroy
    void destroy() {
        if (requester != null) {
            log.info("Disposing RSocket requester for {}:{}", rsocketHost, rsocketPort);
            requester.dispose();
        }
    }

    public void streamAnswer(String userId, String requestId, Flux<String> tokens) {
        log.info("Streaming answer to notification-service via RSocket route 'search.result', userId={}", userId);

        RSocketRequester requester = getOrCreateRequester();

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(Map.of("userId", userId, "requestId", requestId));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize metadata for RSocket", e);
        }

        requester
            .route("search.result")
            // attach JSON metadata as a separate composite metadata entry
            .metadata(metadataJson, MimeType.valueOf("application/json"))
            .data(tokens)
            .retrieveFlux(Void.class)
            .doOnError(err -> log.error("Error streaming to notification-service via RSocket", err))
            .subscribe();
    }
}
