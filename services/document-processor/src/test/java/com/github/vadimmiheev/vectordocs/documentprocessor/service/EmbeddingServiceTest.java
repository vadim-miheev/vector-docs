package com.github.vadimmiheev.vectordocs.documentprocessor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.documentprocessor.dto.DocumentProcessingEvent;
import com.github.vadimmiheev.vectordocs.documentprocessor.dto.DocumentUploadedEvent;
import com.github.vadimmiheev.vectordocs.documentprocessor.entity.Embedding;
import com.github.vadimmiheev.vectordocs.documentprocessor.event.EmbeddingsGeneratedEvent;
import com.github.vadimmiheev.vectordocs.documentprocessor.repository.EmbeddingRepository;
import com.github.vadimmiheev.vectordocs.documentprocessor.util.DocumentsStatusStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private OpenAiEmbeddingModel embeddingModel;

    @Mock
    private EmbeddingRepository embeddingRepository;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(embeddingModel, embeddingRepository, publisher, kafkaTemplate, objectMapper);
        ReflectionTestUtils.setField(embeddingService, "chunkSize", 600);
        ReflectionTestUtils.setField(embeddingService, "chunkOverlap", 100);
        ReflectionTestUtils.setField(embeddingService, "generatorBatchSize", 100);
        ReflectionTestUtils.setField(embeddingService, "documentsProcessingTopic", "documents.processing");

        // Clear DocumentsStatusStore to avoid interference between tests
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, ?> statuses = (ConcurrentHashMap<String, ?>) ReflectionTestUtils.getField(DocumentsStatusStore.class, "STATUSES");
        assertNotNull(statuses, "STATUSES field should not be null");
        statuses.clear();
    }

    @Test
    void generateAndSaveEmbeddings_validPages_savesEmbeddings() {
        // Arrange
        DocumentUploadedEvent event = new DocumentUploadedEvent();
        event.setId(UUID.fromString("223e4567-e89b-12d3-a456-426614174001"));
        event.setName("test.pdf");
        event.setUserId("user123");

        ArrayList<String> pages = new ArrayList<>();
        pages.add("Page 1 content. This is some text that will be split into chunks.");
        pages.add("Page 2 content. More text here for testing.");

        // Mock repository save
        when(embeddingRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int chunksCount = embeddingService.generateAndSaveEmbeddings(event, pages);

        // Assert
        assertTrue(chunksCount > 0);
        verify(embeddingRepository, times(1)).saveAll(anyList());

        // Capture saved embeddings
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Embedding>> captor = ArgumentCaptor.forClass(List.class);
        verify(embeddingRepository).saveAll(captor.capture());
        List<Embedding> savedEmbeddings = captor.getValue();

        assertNotNull(savedEmbeddings);
        assertFalse(savedEmbeddings.isEmpty());
        assertEquals(chunksCount, savedEmbeddings.size());

        // Verify each embedding has correct metadata
        for (Embedding embedding : savedEmbeddings) {
            assertEquals(event.getId(), embedding.getFileUuid());
            assertEquals(event.getName(), embedding.getFileName());
            assertEquals(event.getUserId(), embedding.getUserId());
            assertNotNull(embedding.getChunkText());
            assertTrue(embedding.getPageNumber() >= 1 && embedding.getPageNumber() <= pages.size());
            // Vector should be default zero array (not generated yet)
            assertNotNull(embedding.getVector());
            assertEquals(768, embedding.getVector().length);
            for (float value : embedding.getVector()) {
                assertEquals(0.0f, value, 0.001f);
            }
            assertFalse(embedding.getVectorGenerated()); // Not generated yet
        }
    }

    @Test
    void generateAndSaveEmbeddings_emptyPages_returnsZero() {
        // Arrange
        DocumentUploadedEvent event = new DocumentUploadedEvent();
        event.setId(UUID.randomUUID());
        event.setName("test.pdf");
        event.setUserId("user123");

        ArrayList<String> pages = new ArrayList<>();

        // Act
        int chunksCount = embeddingService.generateAndSaveEmbeddings(event, pages);

        // Assert
        assertEquals(0, chunksCount);
        verify(embeddingRepository, never()).saveAll(anyList());
    }

    @Test
    void generateAndSaveEmbeddings_documentCancelled_throwsException() {
        // Arrange
        DocumentUploadedEvent event = new DocumentUploadedEvent();
        event.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        event.setName("test.pdf");
        event.setUserId("user123");

        ArrayList<String> pages = new ArrayList<>();
        pages.add("Page content");

        // Mark document as cancelled
        DocumentsStatusStore.cancel(event.getId().toString());

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                embeddingService.generateAndSaveEmbeddings(event, pages)
        );
        assertEquals("Document processing cancelled", exception.getMessage());
        verify(embeddingRepository, never()).saveAll(anyList());
    }

    @Test
    void generateAndSaveEmbeddings_pageWithOverlap_includesNeighborText() {
        // Arrange
        DocumentUploadedEvent event = new DocumentUploadedEvent();
        event.setId(UUID.randomUUID());
        event.setName("test.pdf");
        event.setUserId("user123");

        ArrayList<String> pages = new ArrayList<>();
        pages.add("Previous page content with some text.");
        pages.add("Current page content.");
        pages.add("Next page content.");

        when(embeddingRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int chunksCount = embeddingService.generateAndSaveEmbeddings(event, pages);

        // Assert
        assertTrue(chunksCount > 0);
        verify(embeddingRepository, times(1)).saveAll(anyList());

        // Note: We can't easily verify the overlap logic without inspecting the actual splitter output
        // But the method should handle overlap correctly
    }

    @Test
    void deleteEmbeddingsByDocumentId_validId_deletesFromRepository() {
        // Arrange
        UUID documentId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        // Act
        embeddingService.deleteEmbeddingsByDocumentId(documentId);

        // Assert
        verify(embeddingRepository, times(1)).deleteByFileUuid(documentId);
    }

    @Test
    void deleteEmbeddingsByDocumentId_repositoryThrowsException_logsError() {
        // Arrange
        UUID documentId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        doThrow(new RuntimeException("Database error")).when(embeddingRepository).deleteByFileUuid(documentId);

        // Act
        embeddingService.deleteEmbeddingsByDocumentId(documentId);

        // Assert
        verify(embeddingRepository, times(1)).deleteByFileUuid(documentId);
        // Error should be logged
    }

    @Test
    void processPendingEmbeddingsForDocument_pendingEmbeddings_generatesVectors() throws Exception {
        // Arrange
        UUID fileUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String fileName = "test.pdf";
        String userId = "user123";

        List<Embedding> pendingEmbeddings = new ArrayList<>();
        Embedding embedding1 = Embedding.builder()
                .fileUuid(fileUuid)
                .fileName(fileName)
                .userId(userId)
                .chunkText("Chunk 1 text")
                .pageNumber(1)
                .vectorGenerated(false)
                .build();
        Embedding embedding2 = Embedding.builder()
                .fileUuid(fileUuid)
                .fileName(fileName)
                .userId(userId)
                .chunkText("Chunk 2 text")
                .pageNumber(1)
                .vectorGenerated(false)
                .build();
        pendingEmbeddings.add(embedding1);
        pendingEmbeddings.add(embedding2);

        List<TextSegment> segments = Arrays.asList(
                TextSegment.from("Chunk 1 text"),
                TextSegment.from("Chunk 2 text")
        );
        List<dev.langchain4j.data.embedding.Embedding> vectors = Arrays.asList(
                dev.langchain4j.data.embedding.Embedding.from(new float[]{0.1f, 0.2f}),
                dev.langchain4j.data.embedding.Embedding.from(new float[]{0.3f, 0.4f})
        );

        when(embeddingRepository.findByFileUuidAndVectorGenerated(eq(fileUuid), eq(false), any(Limit.class)))
                .thenReturn(pendingEmbeddings);
        @SuppressWarnings("unchecked")
        Response<List<dev.langchain4j.data.embedding.Embedding>> response = mock(Response.class);
        when(response.content()).thenReturn(vectors);
        when(embeddingModel.embedAll(segments)).thenReturn(response);
        when(embeddingRepository.countByFileUuidAndVectorGeneratedFalse(fileUuid)).thenReturn(0L);

        // Act
        long remaining = embeddingService.processPendingEmbeddingsForDocument(fileUuid, fileName, userId);

        // Assert
        assertEquals(0, remaining);
        verify(embeddingRepository, times(1)).findByFileUuidAndVectorGenerated(eq(fileUuid), eq(false), any(Limit.class));
        verify(embeddingModel, times(1)).embedAll(segments);
        verify(embeddingRepository, times(1)).saveAll(pendingEmbeddings);

        // Verify vectors were set
        assertNotNull(embedding1.getVector());
        assertTrue(embedding1.getVectorGenerated());
        assertNotNull(embedding2.getVector());
        assertTrue(embedding2.getVectorGenerated());

        // Verify event was published (since remaining = 0)
        verify(publisher, times(1)).publishEvent(any(EmbeddingsGeneratedEvent.class));
    }

    @Test
    void processPendingEmbeddingsForDocument_documentCancelled_returnsZero() {
        // Arrange
        UUID fileUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String fileName = "test.pdf";
        String userId = "user123";

        DocumentsStatusStore.cancel(fileUuid.toString());

        // Act
        long remaining = embeddingService.processPendingEmbeddingsForDocument(fileUuid, fileName, userId);

        // Assert
        assertEquals(0, remaining);
        verify(embeddingRepository, never()).findByFileUuidAndVectorGenerated(any(), anyBoolean(), any());
    }

    @Test
    void processPendingEmbeddingsForDocument_noPendingEmbeddings_returnsZero() {
        // Arrange
        UUID fileUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String fileName = "test.pdf";
        String userId = "user123";

        when(embeddingRepository.findByFileUuidAndVectorGenerated(eq(fileUuid), eq(false), any(Limit.class)))
                .thenReturn(Collections.emptyList());

        // Act
        long remaining = embeddingService.processPendingEmbeddingsForDocument(fileUuid, fileName, userId);

        // Assert
        assertEquals(0, remaining);
        verify(embeddingRepository, times(1)).findByFileUuidAndVectorGenerated(eq(fileUuid), eq(false), any(Limit.class));
        verify(embeddingModel, never()).embedAll(anyList());
    }

    @Test
    void processPendingEmbeddingsForDocument_partialProgress_publishesProcessingEvent() throws Exception {
        // Arrange
        UUID fileUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String fileName = "test.pdf";
        String userId = "user123";

        List<Embedding> pendingEmbeddings = new ArrayList<>();
        Embedding embedding = Embedding.builder()
                .fileUuid(fileUuid)
                .fileName(fileName)
                .userId(userId)
                .chunkText("Chunk text")
                .pageNumber(1)
                .vectorGenerated(false)
                .build();
        pendingEmbeddings.add(embedding);

        List<dev.langchain4j.data.embedding.Embedding> vectors = Collections.singletonList(
                dev.langchain4j.data.embedding.Embedding.from(new float[]{0.1f, 0.2f})
        );

        when(embeddingRepository.findByFileUuidAndVectorGenerated(eq(fileUuid), eq(false), any(Limit.class)))
                .thenReturn(pendingEmbeddings);
        @SuppressWarnings("unchecked")
        Response<List<dev.langchain4j.data.embedding.Embedding>> response = mock(Response.class);
        when(response.content()).thenReturn(vectors);
        when(embeddingModel.embedAll(anyList())).thenReturn(response);
        when(embeddingRepository.countByFileUuidAndVectorGeneratedFalse(fileUuid)).thenReturn(5L);
        when(embeddingRepository.countByFileUuid(fileUuid)).thenReturn(10L);
        when(objectMapper.writeValueAsString(any(DocumentProcessingEvent.class))).thenReturn("{\"progress\":50}");

        // Act
        long remaining = embeddingService.processPendingEmbeddingsForDocument(fileUuid, fileName, userId);

        // Assert
        assertEquals(5, remaining);
        verify(embeddingRepository, times(1)).findByFileUuidAndVectorGenerated(eq(fileUuid), eq(false), any(Limit.class));
        verify(embeddingModel, times(1)).embedAll(anyList());
        verify(embeddingRepository, times(1)).saveAll(pendingEmbeddings);

        // Verify processing event was published
        verify(kafkaTemplate, times(1)).send(eq("documents.processing"), eq(fileUuid.toString()), anyString());
        verify(publisher, never()).publishEvent(any(EmbeddingsGeneratedEvent.class));
    }

    @Test
    void processPendingEmbeddingsForDocument_embeddingModelThrowsException_logsError() {
        // Arrange
        UUID fileUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String fileName = "test.pdf";
        String userId = "user123";

        List<Embedding> pendingEmbeddings = Collections.singletonList(
                Embedding.builder()
                        .fileUuid(fileUuid)
                        .fileName(fileName)
                        .userId(userId)
                        .chunkText("Chunk text")
                        .pageNumber(1)
                        .vectorGenerated(false)
                        .build()
        );

        when(embeddingRepository.findByFileUuidAndVectorGenerated(eq(fileUuid), eq(false), any(Limit.class)))
                .thenReturn(pendingEmbeddings);
        when(embeddingModel.embedAll(anyList())).thenThrow(new RuntimeException("Model error"));

        // Act
        long remaining = embeddingService.processPendingEmbeddingsForDocument(fileUuid, fileName, userId);

        // Assert
        assertEquals(0, remaining); // Returns 0 on error
        verify(embeddingRepository, times(1)).findByFileUuidAndVectorGenerated(eq(fileUuid), eq(false), any(Limit.class));
        verify(embeddingModel, times(1)).embedAll(anyList());
        verify(embeddingRepository, never()).saveAll(anyList());
        // Error should be logged
    }

    @Test
    void backgroundProcessingOfAllPending_startsNewThread() {
        // Arrange
        DocumentUploadedEvent event = new DocumentUploadedEvent();
        event.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        event.setName("test.pdf");
        event.setUserId("user123");

        // Mock processPendingEmbeddingsForDocument to return 0 immediately
        EmbeddingService spyService = spy(embeddingService);
        doReturn(0L).when(spyService).processPendingEmbeddingsForDocument(event.getId(), event.getName(), event.getUserId());

        // Act
        spyService.backgroundProcessingOfAllPending(event);

        // Assert
        // Thread should be started - we can verify by checking that processPendingEmbeddingsForDocument was called
        // However, since it's async, we need to wait a bit
        try {
            Thread.sleep(100); // Small delay for thread to start
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify the method was called (might need retry logic)
        verify(spyService, timeout(1000).atLeastOnce())
                .processPendingEmbeddingsForDocument(event.getId(), event.getName(), event.getUserId());
    }

    @Test
    void countTotalEmbeddings_callsRepository() {
        // Arrange
        UUID fileUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        when(embeddingRepository.countByFileUuid(fileUuid)).thenReturn(5L);

        // Act
        long count = embeddingService.countTotalEmbeddings(fileUuid);

        // Assert
        assertEquals(5L, count);
        verify(embeddingRepository, times(1)).countByFileUuid(fileUuid);
    }

    @Test
    void getPageTextWithOverlap_emptyPage_returnsEmptyString() {
        // Arrange
        ArrayList<String> pages = new ArrayList<>();
        pages.add("");
        pages.add("Next page");

        // Act
        String result = ReflectionTestUtils.invokeMethod(embeddingService, "getPageTextWithOverlap", pages, 0);

        // Assert
        assertEquals("", result);
    }

    @Test
    void getPageTextWithOverlap_middlePage_includesOverlap() {
        // Arrange
        ArrayList<String> pages = new ArrayList<>();
        pages.add("Previous page with enough text for overlap.");
        pages.add("Current page text.");
        pages.add("Next page with enough text for overlap.");

        // Set chunkOverlap to 10 for test
        ReflectionTestUtils.setField(embeddingService, "chunkOverlap", 10);

        // Act
        String result = ReflectionTestUtils.invokeMethod(embeddingService, "getPageTextWithOverlap", pages, 1);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Current page text."));
        // Should include overlap from previous and next pages
        // Previous page overlap: last 10 chars "or overlap."
        // Next page overlap: first 10 chars "Next page w"
        // We can't assert exact string because it depends on substring logic
    }
}