package com.github.vadimmiheev.vectordocs.documentprocessor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.documentprocessor.dto.DocumentUploadedEvent;
import com.github.vadimmiheev.vectordocs.documentprocessor.util.DocumentsStatusStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingServiceTest {

    @Mock
    private DownloadService downloadService;

    @Mock
    private TextExtractionService textExtractionService;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private DocumentProcessingService service;

    @BeforeEach
    void setUp() {
        service = new DocumentProcessingService(downloadService, textExtractionService, embeddingService, objectMapper, kafkaTemplate);
        // Set topic names via reflection
        ReflectionTestUtils.setField(service, "documentsProcessedTopic", "documents.processed");
        ReflectionTestUtils.setField(service, "documentsProcessingErrorTopic", "documents.processing.error");
        // Clear DocumentsStatusStore to avoid interference between tests
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, ?> statuses = (ConcurrentHashMap<String, ?>) ReflectionTestUtils.getField(DocumentsStatusStore.class, "STATUSES");
        if (statuses != null) {
            statuses.clear();
        }
    }

    @Test
    void process_validEvent_success() throws Exception {
        // Arrange
        DocumentUploadedEvent event = new DocumentUploadedEvent();
        event.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        event.setName("test.pdf");
        event.setSize(1024L);
        event.setUserId("user123");
        event.setContentType("application/pdf");
        event.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        event.setDownloadUrl(URI.create("http://example.com/file.pdf"));

        byte[] fileBytes = new byte[]{1, 2, 3};
        ArrayList<String> pages = new ArrayList<>();
        pages.add("Page 1 content");
        pages.add("Page 2 content");

        when(downloadService.download(event.getDownloadUrl(), event.getUserId())).thenReturn(fileBytes);
        when(textExtractionService.extractText(fileBytes, event.getContentType(), event.getName())).thenReturn(pages);
        when(embeddingService.generateAndSaveEmbeddings(event, pages)).thenReturn(5);

        // Act
        service.process(event);

        // Assert
        verify(downloadService, times(1)).download(event.getDownloadUrl(), event.getUserId());
        verify(textExtractionService, times(1)).extractText(fileBytes, event.getContentType(), event.getName());
        verify(embeddingService, times(1)).generateAndSaveEmbeddings(event, pages);
        verify(embeddingService, times(1)).backgroundProcessingOfAllPending(event);
        // publishDocumentProcessedEvent will be called internally via event listener
    }

    @Test
    void process_documentCancelled_skipsProcessing() throws Exception {
        // Arrange
        DocumentUploadedEvent event = new DocumentUploadedEvent();
        event.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        event.setName("test.pdf");
        event.setUserId("user123");
        event.setDownloadUrl(URI.create("http://example.com/file.pdf"));

        byte[] fileBytes = new byte[]{1, 2, 3};
        when(downloadService.download(event.getDownloadUrl(), event.getUserId())).thenReturn(fileBytes);

        // Mark document as cancelled before processing
        DocumentsStatusStore.cancel(event.getId().toString());

        // Act
        service.process(event);

        // Assert
        verify(downloadService, times(1)).download(event.getDownloadUrl(), event.getUserId());
        verify(textExtractionService, never()).extractText(any(), any(), any());
        verify(embeddingService, never()).generateAndSaveEmbeddings(any(), any());
    }

    @Test
    void process_downloadFails_publishesError() throws Exception {
        // Arrange
        DocumentUploadedEvent event = new DocumentUploadedEvent();
        event.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        event.setName("test.pdf");
        event.setUserId("user123");
        event.setDownloadUrl(URI.create("http://example.com/file.pdf"));

        when(downloadService.download(event.getDownloadUrl(), event.getUserId()))
                .thenThrow(new RuntimeException("Download failed"));
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{\"error\":\"Download failed\"}");

        // Act
        service.process(event);

        // Assert
        verify(downloadService, times(1)).download(event.getDownloadUrl(), event.getUserId());
        verify(textExtractionService, never()).extractText(any(), any(), any());
        verify(kafkaTemplate, times(1)).send(eq("documents.processing.error"), anyString(), anyString());
    }

    @Test
    void process_textExtractionFails_publishesError() throws Exception {
        // Arrange
        DocumentUploadedEvent event = new DocumentUploadedEvent();
        event.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        event.setName("test.pdf");
        event.setUserId("user123");
        event.setContentType("application/pdf");
        event.setDownloadUrl(URI.create("http://example.com/file.pdf"));

        byte[] fileBytes = new byte[]{1, 2, 3};
        when(downloadService.download(event.getDownloadUrl(), event.getUserId())).thenReturn(fileBytes);
        when(textExtractionService.extractText(fileBytes, event.getContentType(), event.getName()))
                .thenThrow(new RuntimeException("Text extraction failed"));
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{\"error\":\"Text extraction failed\"}");

        // Act
        service.process(event);

        // Assert
        verify(downloadService, times(1)).download(event.getDownloadUrl(), event.getUserId());
        verify(textExtractionService, times(1)).extractText(fileBytes, event.getContentType(), event.getName());
        verify(kafkaTemplate, times(1)).send(eq("documents.processing.error"), anyString(), anyString());
    }

    @Test
    void process_noChunksGenerated_publishesProcessedEvent() throws Exception {
        // Arrange
        DocumentUploadedEvent event = new DocumentUploadedEvent();
        event.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        event.setName("test.pdf");
        event.setUserId("user123");
        event.setContentType("application/pdf");
        event.setDownloadUrl(URI.create("http://example.com/file.pdf"));

        byte[] fileBytes = new byte[]{1, 2, 3};
        ArrayList<String> pages = new ArrayList<>();
        pages.add("Page 1 content");

        when(downloadService.download(event.getDownloadUrl(), event.getUserId())).thenReturn(fileBytes);
        when(textExtractionService.extractText(fileBytes, event.getContentType(), event.getName())).thenReturn(pages);
        when(embeddingService.generateAndSaveEmbeddings(event, pages)).thenReturn(0);
        when(embeddingService.countTotalEmbeddings(event.getId())).thenReturn(0L);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"id\":\"" + event.getId() + "\",\"userId\":\"user123\",\"fileName\":\"test.pdf\",\"embeddingsCount\":0}");

        // Act
        service.process(event);

        // Assert
        verify(downloadService, times(1)).download(event.getDownloadUrl(), event.getUserId());
        verify(textExtractionService, times(1)).extractText(fileBytes, event.getContentType(), event.getName());
        verify(embeddingService, times(1)).generateAndSaveEmbeddings(event, pages);
        verify(embeddingService, never()).backgroundProcessingOfAllPending(event);
        // Should publish processed event directly
        verify(kafkaTemplate, times(1)).send(eq("documents.processed"), eq(event.getId().toString()), anyString());
    }

}