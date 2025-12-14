package com.github.vadimmiheev.vectordocs.notificationservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.notificationservice.ws.NotificationSessionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnswerStreamControllerTest {

    private AnswerStreamController controller;

    @Mock
    private NotificationSessionRegistry sessionRegistry;

    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        controller = new AnswerStreamController(sessionRegistry, objectMapper);
    }

    @Test
    void searchResultsHandler_shouldProcessTokensAndSendToWebSocket() throws JsonProcessingException {
        // Given
        String userId = "user123";
        String requestId = "req456";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", userId);
        metadata.put("requestId", requestId);

        Flux<String> tokens = Flux.just("Hello", " ", "world");

        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{\"event\":\"chat.response\",\"requestId\":\"req456\",\"token\":\"Hello\"}",
                "{\"event\":\"chat.response\",\"requestId\":\"req456\",\"token\":\" \"}",
                "{\"event\":\"chat.response\",\"requestId\":\"req456\",\"token\":\"world\"}",
                "{\"event\":\"chat.response\",\"requestId\":\"req456\",\"complete\":true}");
        when(sessionRegistry.sendToUser(eq(userId), anyString())).thenReturn(1);

        // When
        Flux<Void> result = controller.searchResultsHandler(tokens, metadata);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(objectMapper, times(4)).writeValueAsString(any(Map.class));
        verify(sessionRegistry, times(4)).sendToUser(eq(userId), anyString());
    }

    @Test
    void searchResultsHandler_withSourcesInTokens_shouldIncludeSources() throws JsonProcessingException {
        // Given
        String userId = "user123";
        String requestId = "req456";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", userId);
        metadata.put("requestId", requestId);

        // Token with sources
        Flux<String> tokens = Flux.just("Answer: ", "<BEGIN_SOURCES>file123/doc.pdf/1<END_SOURCES>", " is correct");

        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{\"event\":\"chat.response\",\"requestId\":\"req456\",\"token\":\"Answer: \"}",
                "{\"event\":\"chat.response\",\"requestId\":\"req456\",\"token\":\"\",\"sources\":[{\"id\":\"file123\",\"name\":\"doc.pdf\",\"page\":\"1\"}]}",
                "{\"event\":\"chat.response\",\"requestId\":\"req456\",\"token\":\" is correct\"}",
                "{\"event\":\"chat.response\",\"requestId\":\"req456\",\"complete\":true}");
        when(sessionRegistry.sendToUser(eq(userId), anyString())).thenReturn(1);

        // When
        Flux<Void> result = controller.searchResultsHandler(tokens, metadata);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(objectMapper, times(4)).writeValueAsString(any(Map.class));
        verify(sessionRegistry, times(4)).sendToUser(eq(userId), anyString());
    }

    @Test
    void searchResultsHandler_whenJsonProcessingFails_shouldThrowException() throws JsonProcessingException {
        // Given
        String userId = "user123";
        String requestId = "req456";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", userId);
        metadata.put("requestId", requestId);

        Flux<String> tokens = Flux.just("Hello");

        when(objectMapper.writeValueAsString(any(Map.class))).thenThrow(new JsonProcessingException("JSON error") {});

        // When
        Flux<Void> result = controller.searchResultsHandler(tokens, metadata);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(objectMapper, times(1)).writeValueAsString(any(Map.class));
        verify(sessionRegistry, never()).sendToUser(anyString(), anyString());
    }

    @Test
    void searchResultsHandler_whenCompleteMessageFails_shouldLogError() throws JsonProcessingException {
        // Given
        String userId = "user123";
        String requestId = "req456";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", userId);
        metadata.put("requestId", requestId);

        Flux<String> tokens = Flux.just("Hello");

        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{\"event\":\"chat.response\",\"requestId\":\"req456\",\"token\":\"Hello\"}",
                "{\"event\":\"chat.response\",\"requestId\":\"req456\",\"complete\":true}");
        when(sessionRegistry.sendToUser(eq(userId), anyString())).thenReturn(1).thenReturn(0); // Second call fails

        // When
        Flux<Void> result = controller.searchResultsHandler(tokens, metadata);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        // Should complete even if complete message fails to send
        verify(objectMapper, times(2)).writeValueAsString(any(Map.class));
        verify(sessionRegistry, times(2)).sendToUser(eq(userId), anyString());
    }

    @Test
    void searchResultsHandler_withEmptyTokens_shouldSendCompleteMessage() throws JsonProcessingException {
        // Given
        String userId = "user123";
        String requestId = "req456";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", userId);
        metadata.put("requestId", requestId);

        Flux<String> tokens = Flux.empty();

        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{\"event\":\"chat.response\",\"requestId\":\"req456\",\"complete\":true}");
        when(sessionRegistry.sendToUser(eq(userId), anyString())).thenReturn(1);

        // When
        Flux<Void> result = controller.searchResultsHandler(tokens, metadata);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(objectMapper, times(1)).writeValueAsString(any(Map.class));
        verify(sessionRegistry, times(1)).sendToUser(eq(userId), anyString());
    }

    @Test
    void searchResultsHandler_withNullMetadata_shouldHandleGracefully() throws JsonProcessingException {
        // Given
        Map<String, Object> metadata = null;
        Flux<String> tokens = Flux.just("Hello");

        // When
        Flux<Void> result = controller.searchResultsHandler(tokens, metadata);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(objectMapper, never()).writeValueAsString(any(Map.class));
        verify(sessionRegistry, never()).sendToUser(anyString(), anyString());
    }
}