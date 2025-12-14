package com.github.vadimmiheev.vectordocs.notificationservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.notificationservice.ws.NotificationSessionRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentsEventsListenerTest {

    private DocumentsEventsListener listener;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationSessionRegistry sessionRegistry;

    @BeforeEach
    void setUp() {
        listener = new DocumentsEventsListener(objectMapper, sessionRegistry);
    }

    @Test
    void onMessage_withValidMessageAndUserId_shouldSendToWebSocket() throws Exception {
        // Given
        String topic = "documents.uploaded";
        String userId = "user123";
        String messageJson = "{\"userId\":\"" + userId + "\",\"documentId\":\"doc456\"}";

        HashMap<String, String> messageMap = new HashMap<>();
        messageMap.put("userId", userId);
        messageMap.put("documentId", "doc456");

        ConsumerRecord<String, String> record = new ConsumerRecord<>(topic, 0, 0, "key", messageJson);

        when(objectMapper.readValue(messageJson, HashMap.class)).thenReturn(messageMap);
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{\"event\":\"documents.uploaded\",\"userId\":\"user123\",\"documentId\":\"doc456\"}");
        when(sessionRegistry.sendToUser(eq(userId), anyString())).thenReturn(1);

        // When
        listener.onMessage(messageJson, record);

        // Then
        verify(objectMapper, times(1)).readValue(messageJson, HashMap.class);
        verify(objectMapper, times(1)).writeValueAsString(any(Map.class));
        verify(sessionRegistry, times(1)).sendToUser(eq(userId), anyString());
    }

    @Test
    void onMessage_withoutUserId_shouldNotSendToWebSocket() throws Exception {
        // Given
        String topic = "documents.uploaded";
        String messageJson = "{\"documentId\":\"doc456\"}"; // No userId

        HashMap<String, String> messageMap = new HashMap<>();
        messageMap.put("documentId", "doc456");

        ConsumerRecord<String, String> record = new ConsumerRecord<>(topic, 0, 0, "key", messageJson);

        when(objectMapper.readValue(messageJson, HashMap.class)).thenReturn(messageMap);

        // When
        listener.onMessage(messageJson, record);

        // Then
        verify(objectMapper, times(1)).readValue(messageJson, HashMap.class);
        verify(objectMapper, never()).writeValueAsString(any());
        verify(sessionRegistry, never()).sendToUser(anyString(), anyString());
    }

    @Test
    void onMessage_withNullUserId_shouldNotSendToWebSocket() throws Exception {
        // Given
        String topic = "documents.uploaded";
        String messageJson = "{\"userId\":null,\"documentId\":\"doc456\"}";

        HashMap<String, String> messageMap = new HashMap<>();
        messageMap.put("userId", null);
        messageMap.put("documentId", "doc456");

        ConsumerRecord<String, String> record = new ConsumerRecord<>(topic, 0, 0, "key", messageJson);

        when(objectMapper.readValue(messageJson, HashMap.class)).thenReturn(messageMap);

        // When
        listener.onMessage(messageJson, record);

        // Then
        verify(objectMapper, times(1)).readValue(messageJson, HashMap.class);
        verify(objectMapper, never()).writeValueAsString(any());
        verify(sessionRegistry, never()).sendToUser(anyString(), anyString());
    }

    @Test
    void onMessage_whenJsonParsingFails_shouldLogError() throws Exception {
        // Given
        String topic = "documents.uploaded";
        String invalidJson = "invalid json";
        ConsumerRecord<String, String> record = new ConsumerRecord<>(topic, 0, 0, "key", invalidJson);

        when(objectMapper.readValue(invalidJson, HashMap.class)).thenThrow(new RuntimeException("JSON parsing error"));

        // When
        listener.onMessage(invalidJson, record);

        // Then
        verify(objectMapper, times(1)).readValue(invalidJson, HashMap.class);
        verify(objectMapper, never()).writeValueAsString(any());
        verify(sessionRegistry, never()).sendToUser(anyString(), anyString());
    }

    @Test
    void onMessage_whenWebSocketSendFails_shouldLogError() throws Exception {
        // Given
        String topic = "documents.uploaded";
        String userId = "user123";
        String messageJson = "{\"userId\":\"" + userId + "\",\"documentId\":\"doc456\"}";

        HashMap<String, String> messageMap = new HashMap<>();
        messageMap.put("userId", userId);
        messageMap.put("documentId", "doc456");

        ConsumerRecord<String, String> record = new ConsumerRecord<>(topic, 0, 0, "key", messageJson);

        when(objectMapper.readValue(messageJson, HashMap.class)).thenReturn(messageMap);
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{\"event\":\"documents.uploaded\",\"userId\":\"user123\",\"documentId\":\"doc456\"}");
        when(sessionRegistry.sendToUser(eq(userId), anyString())).thenReturn(0); // No active sessions

        // When
        listener.onMessage(messageJson, record);

        // Then
        verify(objectMapper, times(1)).readValue(messageJson, HashMap.class);
        verify(objectMapper, times(1)).writeValueAsString(any(Map.class));
        verify(sessionRegistry, times(1)).sendToUser(eq(userId), anyString());
        // Should log that no active sessions
    }

    @Test
    void onMessage_shouldAddEventTypeToMessage() throws Exception {
        // Given
        String topic = "documents.processed";
        String userId = "user123";
        String messageJson = "{\"userId\":\"" + userId + "\",\"documentId\":\"doc456\"}";

        HashMap<String, String> messageMap = new HashMap<>();
        messageMap.put("userId", userId);
        messageMap.put("documentId", "doc456");

        ConsumerRecord<String, String> record = new ConsumerRecord<>(topic, 0, 0, "key", messageJson);

        when(objectMapper.readValue(messageJson, HashMap.class)).thenReturn(messageMap);
        when(objectMapper.writeValueAsString(any(Map.class))).thenAnswer(invocation -> {
            Map<String, String> map = invocation.getArgument(0);
            // Verify event was added
            assertTrue(map.containsKey("event"));
            assertEquals(topic, map.get("event"));
            return "serialized";
        });
        when(sessionRegistry.sendToUser(eq(userId), anyString())).thenReturn(1);

        // When
        listener.onMessage(messageJson, record);

        // Then
        verify(objectMapper, times(1)).writeValueAsString(any(Map.class));
        // Assertions are in the mock answer above
    }
}