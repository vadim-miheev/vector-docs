package com.github.vadimmiheev.vectordocs.searchservice.util;

import com.github.vadimmiheev.vectordocs.searchservice.entity.Embedding;
import com.github.vadimmiheev.vectordocs.searchservice.dto.SearchRequestEvent;
import com.github.vadimmiheev.vectordocs.searchservice.dto.SearchProcessedEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Factory for creating test data objects for search-service tests.
 */
public class TestDataFactory {

    private TestDataFactory() {
        // Utility class
    }

    /**
     * Creates a test Embedding with default values.
     */
    public static Embedding createEmbedding() {
        return Embedding.builder()
                .fileUuid(UUID.randomUUID())
                .fileName("test-document.pdf")
                .chunkText("This is a test chunk of text for embedding.")
                .vector(new float[768]) // zero vector for simplicity
                .pageNumber(1)
                .createdAt(Instant.now())
                .userId("test-user-id")
                .build();
    }

    /**
     * Creates a test Embedding with custom file UUID.
     */
    public static Embedding createEmbedding(UUID fileUuid) {
        Embedding embedding = createEmbedding();
        embedding.setFileUuid(fileUuid);
        return embedding;
    }

    /**
     * Creates a test Embedding with custom vector.
     */
    public static Embedding createEmbedding(float[] vector) {
        Embedding embedding = createEmbedding();
        embedding.setVector(vector);
        return embedding;
    }

    /**
     * Creates a test SearchRequestEvent with default values.
     */
    public static SearchRequestEvent createSearchRequestEvent() {
        SearchRequestEvent event = new SearchRequestEvent();
        event.setRequestId(UUID.randomUUID().toString());
        event.setDocumentId(UUID.randomUUID());
        event.setQuery("test search query");
        event.setUserId("test-user-id");
        event.setRagQuery("");
        event.setContext(new ArrayList<>());
        return event;
    }

    /**
     * Creates a test SearchProcessedEvent with default values.
     */
    public static SearchProcessedEvent createSearchProcessedEvent() {
        return SearchProcessedEvent.builder()
                .requestId(UUID.randomUUID().toString())
                .query("test search query")
                .userId("test-user-id")
                .context(new ArrayList<>())
                .embeddings(new ArrayList<>())
                .build();
    }

    /**
     * Creates a float array of specified dimension with random values between -1 and 1.
     */
    public static float[] createRandomVector(int dimension) {
        float[] vector = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            vector[i] = (float) (Math.random() * 2 - 1); // range [-1, 1]
        }
        return vector;
    }

    /**
     * Creates a float array of dimension 768 with random values.
     */
    public static float[] createRandomVector768() {
        return createRandomVector(768);
    }
}