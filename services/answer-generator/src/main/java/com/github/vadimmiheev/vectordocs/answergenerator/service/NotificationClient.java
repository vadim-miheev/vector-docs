package com.github.vadimmiheev.vectordocs.answergenerator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${app.notification.stream-url:http://notification-service:8080/answers/stream}")
    private String streamUrl;

    public void streamAnswer(String userId, Flux<String> tokens) {
        String url = streamUrl + "?userId=" + userId;
        log.info("Streaming answer to notification-service: {}", url);
        webClientBuilder.build()
                .post()
                .uri(url)
                .contentType(MediaType.TEXT_PLAIN)
                .body(BodyInserters.fromPublisher(tokens, String.class))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(err -> log.error("Error streaming to notification-service", err))
                .subscribe();
    }
}
