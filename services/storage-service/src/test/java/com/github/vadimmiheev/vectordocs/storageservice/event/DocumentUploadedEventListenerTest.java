package com.github.vadimmiheev.vectordocs.storageservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.storageservice.dto.DocumentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentUploadedEventListenerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    private DocumentUploadedEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new DocumentUploadedEventListener(kafkaTemplate, objectMapper);
        // Set topic via reflection since @Value is not initialized in unit tests
        try {
            var field = DocumentUploadedEventListener.class.getDeclaredField("documentsUploadedTopic");
            field.setAccessible(true);
            field.set(listener, "documents.uploaded");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void onDocumentUploaded_ShouldSendEventToKafka() throws Exception {
        // Given
        String documentId = "doc-123";
        String userId = "user-456";
        DocumentResponse document = new DocumentResponse(
                documentId,
                "test.pdf",
                1024L,
                userId,
                "application/pdf",
                Instant.now(),
                "http://localhost:8080/documents/doc-123/download",
                "uploaded"
        );
        DocumentUploadedEvent event = new DocumentUploadedEvent(document);

        String expectedJson = "{\"id\":\"doc-123\",\"name\":\"test.pdf\",\"size\":1024,\"userId\":\"user-456\",\"contentType\":\"application/pdf\",\"createdAt\":\"2023-01-01T00:00:00Z\",\"downloadUrl\":\"http://localhost:8080/documents/doc-123/download\",\"status\":\"uploaded\"}";
        when(objectMapper.writeValueAsString(document)).thenReturn(expectedJson);

        // When
        listener.onDocumentUploaded(event);

        // Then
        verify(kafkaTemplate).send(eq("documents.uploaded"), eq(documentId), messageCaptor.capture());
        String sentMessage = messageCaptor.getValue();
        assertEquals(expectedJson, sentMessage);
        verify(objectMapper).writeValueAsString(document);
    }

    @Test
    void onDocumentUploaded_WhenJsonSerializationFails_ShouldLogError() throws Exception {
        // Given
        DocumentResponse document = new DocumentResponse(
                "doc-123",
                "test.pdf",
                1024L,
                "user-456",
                "application/pdf",
                Instant.now(),
                "http://localhost:8080/documents/doc-123/download",
                "uploaded"
        );
        DocumentUploadedEvent event = new DocumentUploadedEvent(document);

        when(objectMapper.writeValueAsString(document)).thenThrow(new RuntimeException("JSON serialization failed"));

        // When
        listener.onDocumentUploaded(event);

        // Then
        verify(kafkaTemplate, never()).send(any(), any(), any());
        verify(objectMapper).writeValueAsString(document);
        // Error should be logged but not rethrown
    }

    @Test
    void onDocumentUploaded_WhenKafkaSendFails_ShouldLogError() throws Exception {
        // Given
        DocumentResponse document = new DocumentResponse(
                "doc-123",
                "test.pdf",
                1024L,
                "user-456",
                "application/pdf",
                Instant.now(),
                "http://localhost:8080/documents/doc-123/download",
                "uploaded"
        );
        DocumentUploadedEvent event = new DocumentUploadedEvent(document);

        String expectedJson = "{\"id\":\"doc-123\"}";
        when(objectMapper.writeValueAsString(document)).thenReturn(expectedJson);
        doThrow(new RuntimeException("Kafka unavailable")).when(kafkaTemplate).send(any(), any(), any());

        // When
        listener.onDocumentUploaded(event);

        // Then
        verify(kafkaTemplate).send(eq("documents.uploaded"), eq("doc-123"), eq(expectedJson));
        verify(objectMapper).writeValueAsString(document);
        // Error should be logged but not rethrown
    }
}