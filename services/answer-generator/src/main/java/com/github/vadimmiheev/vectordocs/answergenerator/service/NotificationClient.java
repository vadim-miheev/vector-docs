package com.github.vadimmiheev.vectordocs.answergenerator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationClient {

    private final RSocketRequester.Builder rSocketRequesterBuilder;

    @Value("${app.notification.rsocket.host:notification-service}")
    private String rsocketHost;

    @Value("${app.notification.rsocket.port:7000}")
    private int rsocketPort;

    public void streamAnswer(String userId, Flux<String> tokens) {
        log.info("Streaming answer to notification-service via RSocket route 'search.result', userId={}", userId);

        RSocketRequester requester = rSocketRequesterBuilder
            .tcp(rsocketHost, rsocketPort);

        requester
            .route("search.result")
            .data(tokens)
            .retrieveFlux(Void.class)
            .doOnError(err -> log.error("Error streaming to notification-service via RSocket", err))
            .subscribe();
    }
}
