package com.github.vadimmiheev.vectordocs.searchservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.searchservice.dto.SearchProcessedEvent;
import com.github.vadimmiheev.vectordocs.searchservice.dto.SearchRequestEvent;
import com.github.vadimmiheev.vectordocs.searchservice.entity.Embedding;
import com.github.vadimmiheev.vectordocs.searchservice.repository.EmbeddingRepository;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchRequestListenerTest {

    @Mock
    private EmbeddingRepository embeddingRepository;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<String> kafkaMessageCaptor;

    @Captor
    private ArgumentCaptor<String> kafkaKeyCaptor;

    private SearchRequestListener searchRequestListener;

    private final String processedTopic = "search.processed";
    private final int topK = 5;

    @BeforeEach
    void setUp() {
        searchRequestListener = new SearchRequestListener(
                embeddingRepository,
                embeddingModel,
                kafkaTemplate,
                objectMapper
        );

        // Set up fields via reflection since they're @Value injected
        try {
            var processedTopicField = SearchRequestListener.class.getDeclaredField("processedTopic");
            processedTopicField.setAccessible(true);
            processedTopicField.set(searchRequestListener, processedTopic);

            var topKField = SearchRequestListener.class.getDeclaredField("topK");
            topKField.setAccessible(true);
            topKField.set(searchRequestListener, topK);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldProcessSearchRequestAndReturnTopSimilarEmbeddings() throws Exception {
        // Given
        String userId = "user-123";
        String query = "test query";
        String requestId = "req-456";
        UUID documentId = null;
        String ragQuery = "enhanced test query";

        SearchRequestEvent requestEvent = new SearchRequestEvent();
        requestEvent.setRequestId(requestId);
        requestEvent.setDocumentId(documentId);
        requestEvent.setQuery(query);
        requestEvent.setRagQuery(ragQuery);
        requestEvent.setUserId(userId);
        requestEvent.setContext(new ArrayList<>());

        String message = "{\"requestId\":\"req-456\",\"query\":\"test query\",\"ragQuery\":\"enhanced test query\",\"userId\":\"user-123\"}";
        String key = "test-key";

        // Mock embedding generation
        float[] queryVector = new float[768];
        Arrays.fill(queryVector, 0.5f);
        String pgVectorString = Arrays.toString(queryVector);

        dev.langchain4j.data.embedding.Embedding langChainEmbedding = dev.langchain4j.data.embedding.Embedding.from(queryVector);
        Response<dev.langchain4j.data.embedding.Embedding> embeddingResponse = Response.from(langChainEmbedding);

        when(objectMapper.readValue(message, SearchRequestEvent.class)).thenReturn(requestEvent);
        when(embeddingModel.embed(ragQuery)).thenReturn(embeddingResponse);

        // Mock repository response
        List<Embedding> mockEmbeddings = Arrays.asList(
                createEmbedding(1L, UUID.randomUUID(), "doc1.pdf", "First chunk text", 1, userId),
                createEmbedding(2L, UUID.randomUUID(), "doc2.pdf", "Second chunk text", 2, userId)
        );

        when(embeddingRepository.findTopSimilar(eq(userId), eq(pgVectorString), eq(Limit.of(topK))))
                .thenReturn(mockEmbeddings);

        // Mock Kafka message serialization
        when(objectMapper.writeValueAsString(any(SearchProcessedEvent.class))).thenReturn("processed-message");

        // When
        searchRequestListener.onSearchRequest(message, key);

        // Then
        // Verify embedding generation
        verify(embeddingModel).embed(ragQuery);

        // Verify repository call
        verify(embeddingRepository).findTopSimilar(userId, pgVectorString, Limit.of(topK));
        verify(embeddingRepository, never()).findTopSimilarByDoc(any(), any(), any(), any());

        // Verify Kafka message sent
        verify(kafkaTemplate).send(eq(processedTopic), eq(key), eq("processed-message"));

        // Verify the processed event structure
        ArgumentCaptor<SearchProcessedEvent> eventCaptor = ArgumentCaptor.forClass(SearchProcessedEvent.class);
        verify(objectMapper).writeValueAsString(eventCaptor.capture());

        SearchProcessedEvent processedEvent = eventCaptor.getValue();
        assertThat(processedEvent.getRequestId()).isEqualTo(requestId);
        assertThat(processedEvent.getQuery()).isEqualTo(query);
        assertThat(processedEvent.getUserId()).isEqualTo(userId);
        assertThat(processedEvent.getEmbeddings()).hasSize(2);

        // Verify first hit
        SearchProcessedEvent.Hit firstHit = processedEvent.getEmbeddings().getFirst();
        assertThat(firstHit.getFileName()).isEqualTo("doc1.pdf");
        assertThat(firstHit.getChunkText()).isEqualTo("First chunk text");
        assertThat(firstHit.getPageNumber()).isEqualTo(1);

        // Verify second hit
        SearchProcessedEvent.Hit secondHit = processedEvent.getEmbeddings().get(1);
        assertThat(secondHit.getFileName()).isEqualTo("doc2.pdf");
        assertThat(secondHit.getChunkText()).isEqualTo("Second chunk text");
        assertThat(secondHit.getPageNumber()).isEqualTo(2);
    }

    @Test
    void shouldProcessSearchRequestWithDocumentIdFilter() throws Exception {
        // Given
        String userId = "user-123";
        String query = "test query";
        String requestId = "req-456";
        UUID documentId = UUID.randomUUID();
        String ragQuery = "enhanced test query";

        SearchRequestEvent requestEvent = new SearchRequestEvent();
        requestEvent.setRequestId(requestId);
        requestEvent.setDocumentId(documentId);
        requestEvent.setQuery(query);
        requestEvent.setRagQuery(ragQuery);
        requestEvent.setUserId(userId);
        requestEvent.setContext(new ArrayList<>());

        String message = "{\"requestId\":\"req-456\",\"query\":\"test query\",\"ragQuery\":\"enhanced test query\",\"userId\":\"user-123\",\"documentId\":\"" + documentId + "\"}";
        String key = "test-key";

        // Mock embedding generation
        float[] queryVector = new float[768];
        Arrays.fill(queryVector, 0.5f);
        String pgVectorString = Arrays.toString(queryVector);

        dev.langchain4j.data.embedding.Embedding langChainEmbedding = dev.langchain4j.data.embedding.Embedding.from(queryVector);
        Response<dev.langchain4j.data.embedding.Embedding> embeddingResponse = Response.from(langChainEmbedding);

        when(objectMapper.readValue(message, SearchRequestEvent.class)).thenReturn(requestEvent);
        when(embeddingModel.embed(ragQuery)).thenReturn(embeddingResponse);

        // Mock repository response with document filter
        List<Embedding> mockEmbeddings = Collections.singletonList(
                createEmbedding(1L, documentId, "doc1.pdf", "Filtered chunk text", 1, userId)
        );

        when(embeddingRepository.findTopSimilarByDoc(eq(userId), eq(pgVectorString), eq(documentId), eq(Limit.of(topK))))
                .thenReturn(mockEmbeddings);

        // Mock Kafka message serialization
        when(objectMapper.writeValueAsString(any(SearchProcessedEvent.class))).thenReturn("processed-message");

        // When
        searchRequestListener.onSearchRequest(message, key);

        // Then
        // Verify repository call with document filter
        verify(embeddingRepository).findTopSimilarByDoc(userId, pgVectorString, documentId, Limit.of(topK));
        verify(embeddingRepository, never()).findTopSimilar(any(), any(), any());

        // Verify Kafka message sent
        verify(kafkaTemplate).send(eq(processedTopic), eq(key), eq("processed-message"));
    }

    @Test
    void shouldSkipProcessingWhenUserIdMissing() throws Exception {
        // Given
        SearchRequestEvent requestEvent = new SearchRequestEvent();
        requestEvent.setRequestId("req-456");
        requestEvent.setQuery("test query");
        requestEvent.setUserId(null); // Missing userId
        requestEvent.setContext(new ArrayList<>());

        String message = "{\"requestId\":\"req-456\",\"query\":\"test query\"}";
        String key = "test-key";

        when(objectMapper.readValue(message, SearchRequestEvent.class)).thenReturn(requestEvent);

        // When
        searchRequestListener.onSearchRequest(message, key);

        // Then
        // Should skip processing without errors
        verify(embeddingModel, never()).embed(any(String.class));
        verify(embeddingRepository, never()).findTopSimilar(any(), any(), any());
        verify(embeddingRepository, never()).findTopSimilarByDoc(any(), any(), any(), any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void shouldSkipProcessingWhenQueryMissing() throws Exception {
        // Given
        SearchRequestEvent requestEvent = new SearchRequestEvent();
        requestEvent.setRequestId("req-456");
        requestEvent.setQuery(""); // Empty query
        requestEvent.setUserId("user-123");
        requestEvent.setContext(new ArrayList<>());

        String message = "{\"requestId\":\"req-456\",\"query\":\"\",\"userId\":\"user-123\"}";
        String key = "test-key";

        when(objectMapper.readValue(message, SearchRequestEvent.class)).thenReturn(requestEvent);

        // When
        searchRequestListener.onSearchRequest(message, key);

        // Then
        // Should skip processing without errors
        verify(embeddingModel, never()).embed(any(String.class));
        verify(embeddingRepository, never()).findTopSimilar(any(), any(), any());
        verify(embeddingRepository, never()).findTopSimilarByDoc(any(), any(), any(), any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void shouldUseQueryWhenRagQueryIsEmpty() throws Exception {
        // Given
        String userId = "user-123";
        String query = "original query";
        String requestId = "req-456";
        UUID documentId = null;
        String ragQuery = ""; // Empty ragQuery

        SearchRequestEvent requestEvent = new SearchRequestEvent();
        requestEvent.setRequestId(requestId);
        requestEvent.setDocumentId(documentId);
        requestEvent.setQuery(query);
        requestEvent.setRagQuery(ragQuery);
        requestEvent.setUserId(userId);
        requestEvent.setContext(new ArrayList<>());

        String message = "{\"requestId\":\"req-456\",\"query\":\"original query\",\"ragQuery\":\"\",\"userId\":\"user-123\"}";
        String key = "test-key";

        // Mock embedding generation - should use query (not ragQuery) since ragQuery is empty
        float[] queryVector = new float[768];
        Arrays.fill(queryVector, 0.5f);
        String pgVectorString = Arrays.toString(queryVector);

        dev.langchain4j.data.embedding.Embedding langChainEmbedding = dev.langchain4j.data.embedding.Embedding.from(queryVector);
        Response<dev.langchain4j.data.embedding.Embedding> embeddingResponse = Response.from(langChainEmbedding);

        when(objectMapper.readValue(message, SearchRequestEvent.class)).thenReturn(requestEvent);
        when(embeddingModel.embed(query)).thenReturn(embeddingResponse); // Should use query, not ragQuery

        // Mock repository response
        List<Embedding> mockEmbeddings = Collections.singletonList(
                createEmbedding(1L, UUID.randomUUID(), "doc.pdf", "Chunk text", 1, userId)
        );

        when(embeddingRepository.findTopSimilar(eq(userId), eq(pgVectorString), eq(Limit.of(topK))))
                .thenReturn(mockEmbeddings);

        // Mock Kafka message serialization
        when(objectMapper.writeValueAsString(any(SearchProcessedEvent.class))).thenReturn("processed-message");

        // When
        searchRequestListener.onSearchRequest(message, key);

        // Then
        // Verify embedding generation uses query (not ragQuery)
        verify(embeddingModel).embed(query);

        // Verify repository call
        verify(embeddingRepository).findTopSimilar(userId, pgVectorString, Limit.of(topK));

        // Verify Kafka message sent
        verify(kafkaTemplate).send(eq(processedTopic), eq(key), eq("processed-message"));
    }

    @Test
    void shouldHandleExceptionGracefully() throws Exception {
        // Given
        String message = "invalid-json";
        String key = "test-key";

        when(objectMapper.readValue(message, SearchRequestEvent.class))
                .thenThrow(new RuntimeException("JSON parsing error"));

        // When
        searchRequestListener.onSearchRequest(message, key);

        // Then
        // Should not throw exception, just log error
        verify(embeddingModel, never()).embed(any(String.class));
        verify(embeddingRepository, never()).findTopSimilar(any(), any(), any());
        verify(embeddingRepository, never()).findTopSimilarByDoc(any(), any(), any(), any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    private Embedding createEmbedding(Long id, UUID fileUuid, String fileName, String chunkText, Integer pageNumber, String userId) {
        return Embedding.builder()
                .id(id)
                .fileUuid(fileUuid)
                .fileName(fileName)
                .chunkText(chunkText)
                .vector(new float[768])
                .pageNumber(pageNumber)
                .createdAt(Instant.now())
                .userId(userId)
                .build();
    }
}