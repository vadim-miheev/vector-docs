package com.github.vadimmiheev.vectordocs.storageservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentDeletedEventListenerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    private DocumentDeletedEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new DocumentDeletedEventListener(kafkaTemplate, objectMapper);
        // Set topic via reflection since @Value is not initialized in unit tests
        try {
            var field = DocumentDeletedEventListener.class.getDeclaredField("documentsDeletedTopic");
            field.setAccessible(true);
            field.set(listener, "documents.deleted");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void onDocumentDeleted_ShouldSendEventToKafka() throws Exception {
        // Given
        String documentId = "doc-123";
        String userId = "user-456";
        DocumentDeletedEvent event = new DocumentDeletedEvent(documentId, userId);

        String expectedJson = "{\"documentId\":\"doc-123\",\"userId\":\"user-456\"}";
        when(objectMapper.writeValueAsString(event)).thenReturn(expectedJson);

        // When
        listener.onDocumentDeleted(event);

        // Then
        verify(kafkaTemplate).send(eq("documents.deleted"), eq(documentId), messageCaptor.capture());
        String sentMessage = messageCaptor.getValue();
        assertEquals(expectedJson, sentMessage);
        verify(objectMapper).writeValueAsString(event);
    }

    @Test
    void onDocumentDeleted_WhenJsonSerializationFails_ShouldLogError() throws Exception {
        // Given
        DocumentDeletedEvent event = new DocumentDeletedEvent("doc-123", "user-456");

        when(objectMapper.writeValueAsString(event)).thenThrow(new RuntimeException("JSON serialization failed"));

        // When
        listener.onDocumentDeleted(event);

        // Then
        verify(kafkaTemplate, never()).send(any(), any(), any());
        verify(objectMapper).writeValueAsString(event);
        // Error should be logged but not rethrown
    }

    @Test
    void onDocumentDeleted_WhenKafkaSendFails_ShouldLogError() throws Exception {
        // Given
        DocumentDeletedEvent event = new DocumentDeletedEvent("doc-123", "user-456");

        String expectedJson = "{\"documentId\":\"doc-123\",\"userId\":\"user-456\"}";
        when(objectMapper.writeValueAsString(event)).thenReturn(expectedJson);
        doThrow(new RuntimeException("Kafka unavailable")).when(kafkaTemplate).send(any(), any(), any());

        // When
        listener.onDocumentDeleted(event);

        // Then
        verify(kafkaTemplate).send(eq("documents.deleted"), eq("doc-123"), eq(expectedJson));
        verify(objectMapper).writeValueAsString(event);
        // Error should be logged but not rethrown
    }
}