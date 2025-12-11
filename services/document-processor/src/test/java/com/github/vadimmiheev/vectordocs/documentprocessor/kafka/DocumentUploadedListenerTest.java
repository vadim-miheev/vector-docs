package com.github.vadimmiheev.vectordocs.documentprocessor.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.documentprocessor.dto.DocumentUploadedEvent;
import com.github.vadimmiheev.vectordocs.documentprocessor.service.DocumentProcessingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentUploadedListenerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private DocumentProcessingService processingService;

    @Mock
    private Acknowledgment acknowledgment;

    private DocumentUploadedListener listener;

    @BeforeEach
    void setUp() {
        listener = new DocumentUploadedListener(objectMapper, processingService);
    }

    @Test
    void onMessage_validMessage_callsProcessingService() throws Exception {
        // Arrange
        String message = "{\"id\":\"123e4567-e89b-12d3-a456-426614174000\",\"name\":\"test.pdf\",\"size\":1024,\"userId\":\"user123\",\"contentType\":\"application/pdf\",\"createdAt\":\"2025-01-01T00:00:00Z\",\"downloadUrl\":\"http://example.com/file.pdf\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("documents.uploaded", 0, 0, "key", message);

        DocumentUploadedEvent expectedEvent = new DocumentUploadedEvent();
        expectedEvent.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        expectedEvent.setName("test.pdf");
        expectedEvent.setSize(1024L);
        expectedEvent.setUserId("user123");
        expectedEvent.setContentType("application/pdf");
        expectedEvent.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        expectedEvent.setDownloadUrl(URI.create("http://example.com/file.pdf"));

        when(objectMapper.readValue(message, DocumentUploadedEvent.class)).thenReturn(expectedEvent);

        // Act
        listener.onMessage(message, record);

        // Assert
        verify(objectMapper, times(1)).readValue(message, DocumentUploadedEvent.class);
        verify(processingService, times(1)).process(expectedEvent);
    }

    @Test
    void onMessage_invalidJson_logsError() throws Exception {
        // Arrange
        String message = "invalid json";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("documents.uploaded", 0, 0, "key", message);

        when(objectMapper.readValue(message, DocumentUploadedEvent.class))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Invalid JSON") {});

        // Act
        listener.onMessage(message, record);

        // Assert
        verify(objectMapper, times(1)).readValue(message, DocumentUploadedEvent.class);
        verify(processingService, never()).process(any());
        // Error should be logged - we can verify logging if needed
    }

    @Test
    void onMessage_processingServiceThrowsException_logsError() throws Exception {
        // Arrange
        String message = "{\"id\":\"123e4567-e89b-12d3-a456-426614174000\",\"name\":\"test.pdf\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("documents.uploaded", 0, 0, "key", message);

        DocumentUploadedEvent expectedEvent = new DocumentUploadedEvent();
        expectedEvent.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        expectedEvent.setName("test.pdf");

        when(objectMapper.readValue(message, DocumentUploadedEvent.class)).thenReturn(expectedEvent);
        doThrow(new RuntimeException("Processing failed")).when(processingService).process(expectedEvent);

        // Act
        listener.onMessage(message, record);

        // Assert
        verify(objectMapper, times(1)).readValue(message, DocumentUploadedEvent.class);
        verify(processingService, times(1)).process(expectedEvent);
        // Error should be logged
    }
}