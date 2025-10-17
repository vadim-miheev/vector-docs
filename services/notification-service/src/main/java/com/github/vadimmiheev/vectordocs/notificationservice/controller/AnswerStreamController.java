package com.github.vadimmiheev.vectordocs.notificationservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.notificationservice.util.SourcesParser;
import com.github.vadimmiheev.vectordocs.notificationservice.ws.NotificationSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
public class AnswerStreamController {

    private final NotificationSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @MessageMapping("search.result")
    public Flux<Void> searchResultsHandler(Flux<String> tokens, @Header(name = "metadata", required = false) Map<String, Object> metadata) {
        String userId = (String) metadata.get("userId");

        return tokens
                .doOnNext((t) -> {
                    String requestId = metadata.get("requestId").toString();
                    t = SourcesParser.processNextToken(requestId, t);

                    Map<String, Object> payload = Map.of(
                            "event", "chat.response",
                            "requestId", requestId,
                            "token", t
                    );

                    if (SourcesParser.isSourcesReady(requestId)) {
                        HashMap<String, Object> temp = new HashMap<>(payload);
                        temp.put("sources", SourcesParser.getSources(requestId));
                        payload = temp;
                    }

                    try {
                        sessionRegistry.sendToUser(userId, objectMapper.writeValueAsString(payload));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .doOnComplete(() -> {
                    Map<String, Object> payload = Map.of(
                            "event", "chat.response",
                            "requestId", metadata.get("requestId"),
                            "complete", true
                    );
                    try {
                        sessionRegistry.sendToUser(userId, objectMapper.writeValueAsString(payload));
                        log.info("Completed streaming chat.message for userId={}", userId);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to send chat.response.complete message for userId={}", userId, e);
                    }
                })
                .then()
                .flux();
    }
}
