package com.github.vadimmiheev.vectordocs.documentprocessor.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.documentprocessor.dto.DocumentDeletedEvent;
import com.github.vadimmiheev.vectordocs.documentprocessor.service.EmbeddingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentDeletedListenerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EmbeddingService embeddingService;

    private DocumentDeletedListener listener;

    @BeforeEach
    void setUp() {
        listener = new DocumentDeletedListener(objectMapper, embeddingService);
    }

    @Test
    void onMessage_validMessage_callsServices() throws Exception {
        // Arrange
        String message = "{\"documentId\":\"123e4567-e89b-12d3-a456-426614174000\",\"userId\":\"user123\",\"deletedAt\":\"2025-01-01T00:00:00Z\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("documents.deleted", 0, 0, "key", message);

        DocumentDeletedEvent expectedEvent = new DocumentDeletedEvent();
        expectedEvent.setDocumentId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        expectedEvent.setUserId("user123");
        expectedEvent.setDeletedAt(Instant.parse("2025-01-01T00:00:00Z"));

        when(objectMapper.readValue(message, DocumentDeletedEvent.class)).thenReturn(expectedEvent);

        // Act
        listener.onMessage(message, record);

        // Assert
        verify(objectMapper, times(1)).readValue(message, DocumentDeletedEvent.class);
        // Verify DocumentsStatusStore.cancel() is called (static method)
        // We can't verify static methods directly, but we can check that embeddingService.deleteEmbeddingsByDocumentId is called
        verify(embeddingService, times(1)).deleteEmbeddingsByDocumentId(expectedEvent.getDocumentId());
    }

    @Test
    void onMessage_invalidJson_logsError() throws Exception {
        // Arrange
        String message = "invalid json";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("documents.deleted", 0, 0, "key", message);

        when(objectMapper.readValue(message, DocumentDeletedEvent.class))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Invalid JSON") {});

        // Act
        listener.onMessage(message, record);

        // Assert
        verify(objectMapper, times(1)).readValue(message, DocumentDeletedEvent.class);
        verify(embeddingService, never()).deleteEmbeddingsByDocumentId(any());
    }

    @Test
    void onMessage_embeddingServiceThrowsException_logsError() throws Exception {
        // Arrange
        String message = "{\"documentId\":\"123e4567-e89b-12d3-a456-426614174000\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("documents.deleted", 0, 0, "key", message);

        DocumentDeletedEvent expectedEvent = new DocumentDeletedEvent();
        expectedEvent.setDocumentId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        when(objectMapper.readValue(message, DocumentDeletedEvent.class)).thenReturn(expectedEvent);
        doThrow(new RuntimeException("Delete failed")).when(embeddingService).deleteEmbeddingsByDocumentId(expectedEvent.getDocumentId());

        // Act
        listener.onMessage(message, record);

        // Assert
        verify(objectMapper, times(1)).readValue(message, DocumentDeletedEvent.class);
        verify(embeddingService, times(1)).deleteEmbeddingsByDocumentId(expectedEvent.getDocumentId());
        // Error should be logged
    }
}