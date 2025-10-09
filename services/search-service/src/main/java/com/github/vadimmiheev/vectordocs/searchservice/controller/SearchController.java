package com.github.vadimmiheev.vectordocs.searchservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.searchservice.dto.SearchRequestEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;

@RestController
@Slf4j
public class SearchController {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.topics.search-request}")
    private String searchRequestTopic;

    public SearchController(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @PostMapping({"/search", "/"})
    public ResponseEntity<?> submitSearch(@RequestBody SearchRequestEvent request,
                                          @RequestHeader(value = "X-User-Id") String userIdHeader) {
        if (StringUtils.hasText(userIdHeader)) {
            request.setUserId(userIdHeader); // User ID rewrite from header
        } else {
            log.error("Missing userId (header X-User-Id)");
            return ResponseEntity.badRequest().build();
        }

        if (!StringUtils.hasText(request.getQuery())) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "query is required"));
        }

        if (request.getContext() == null) {
            request.setContext(new ArrayList<>());
        }

        try {
            String payload = objectMapper.writeValueAsString(request);
            kafkaTemplate.send(searchRequestTopic, request.getUserId(), payload);
            log.info("Published search request to topic '{}' for userId={}", searchRequestTopic, request.getUserId());
        } catch (Exception ex) {
            log.error("Failed to publish search request to topic '{}' for userId={}", searchRequestTopic, request.getUserId(), ex);
            return ResponseEntity.internalServerError().body(Collections.singletonMap("error", "Failed to publish search request"));
        }

        return ResponseEntity.accepted().build();
    }
}
