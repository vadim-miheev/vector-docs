package com.github.vadimmiheev.vectordocs.searchservice.repository;

import com.github.vadimmiheev.vectordocs.searchservice.entity.Embedding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingRepositoryUnitTest {

    @Mock
    private EmbeddingRepository embeddingRepository;

    @Test
    void shouldCallFindTopSimilarWithCorrectParameters() {
        // Given
        String userId = "user-123";
        String queryVector = "[0.1, 0.2, 0.3]";
        int topK = 5;
        Limit limit = Limit.of(topK);

        List<Embedding> expectedEmbeddings = Collections.singletonList(
                Embedding.builder()
                        .id(1L)
                        .fileUuid(UUID.randomUUID())
                        .fileName("doc1.pdf")
                        .chunkText("First chunk")
                        .vector(new float[768])
                        .pageNumber(1)
                        .userId(userId)
                        .build()
        );

        when(embeddingRepository.findTopSimilar(eq(userId), eq(queryVector), eq(limit)))
                .thenReturn(expectedEmbeddings);

        // When
        List<Embedding> result = embeddingRepository.findTopSimilar(userId, queryVector, limit);

        // Then
        assertThat(result).isEqualTo(expectedEmbeddings);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getUserId()).isEqualTo(userId);
        assertThat(result.getFirst().getFileName()).isEqualTo("doc1.pdf");
    }

    @Test
    void shouldCallFindTopSimilarByDocWithCorrectParameters() {
        // Given
        String userId = "user-123";
        String queryVector = "[0.1, 0.2, 0.3]";
        UUID documentId = UUID.randomUUID();
        int topK = 3;
        Limit limit = Limit.of(topK);

        List<Embedding> expectedEmbeddings = Collections.singletonList(
                Embedding.builder()
                        .id(1L)
                        .fileUuid(documentId)
                        .fileName("doc1.pdf")
                        .chunkText("Filtered chunk")
                        .vector(new float[768])
                        .pageNumber(1)
                        .userId(userId)
                        .build()
        );

        when(embeddingRepository.findTopSimilarByDoc(eq(userId), eq(queryVector), eq(documentId), eq(limit)))
                .thenReturn(expectedEmbeddings);

        // When
        List<Embedding> result = embeddingRepository.findTopSimilarByDoc(userId, queryVector, documentId, limit);

        // Then
        assertThat(result).isEqualTo(expectedEmbeddings);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getUserId()).isEqualTo(userId);
        assertThat(result.getFirst().getFileUuid()).isEqualTo(documentId);
        assertThat(result.getFirst().getFileName()).isEqualTo("doc1.pdf");
    }

    @Test
    void shouldHandleEmptyResults() {
        // Given
        String userId = "user-123";
        String queryVector = "[0.1, 0.2, 0.3]";
        Limit limit = Limit.of(5);

        when(embeddingRepository.findTopSimilar(eq(userId), eq(queryVector), eq(limit)))
                .thenReturn(List.of());

        // When
        List<Embedding> result = embeddingRepository.findTopSimilar(userId, queryVector, limit);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleVectorStringFormat() {
        // Given: Test different vector string formats
        String userId = "user-123";
        String queryVector1 = "[0.1, 0.2, 0.3]";
        String queryVector2 = "[0.5, 0.6, 0.7]";
        Limit limit = Limit.of(5);

        Embedding embedding1 = Embedding.builder()
                .id(1L)
                .fileUuid(UUID.randomUUID())
                .fileName("doc1.pdf")
                .chunkText("Chunk 1")
                .vector(new float[768])
                .pageNumber(1)
                .userId(userId)
                .build();

        Embedding embedding2 = Embedding.builder()
                .id(2L)
                .fileUuid(UUID.randomUUID())
                .fileName("doc2.pdf")
                .chunkText("Chunk 2")
                .vector(new float[768])
                .pageNumber(2)
                .userId(userId)
                .build();

        when(embeddingRepository.findTopSimilar(eq(userId), eq(queryVector1), eq(limit)))
                .thenReturn(List.of(embedding1));

        when(embeddingRepository.findTopSimilar(eq(userId), eq(queryVector2), eq(limit)))
                .thenReturn(List.of(embedding2));

        // When & Then: Test first vector
        List<Embedding> result1 = embeddingRepository.findTopSimilar(userId, queryVector1, limit);
        assertThat(result1).hasSize(1);
        assertThat(result1.getFirst().getFileName()).isEqualTo("doc1.pdf");

        // When & Then: Test second vector
        List<Embedding> result2 = embeddingRepository.findTopSimilar(userId, queryVector2, limit);
        assertThat(result2).hasSize(1);
        assertThat(result2.getFirst().getFileName()).isEqualTo("doc2.pdf");
    }

    @Test
    void shouldRespectLimitParameter() {
        // Given
        String userId = "user-123";
        String queryVector = "[0.1, 0.2, 0.3]";
        Limit smallLimit = Limit.of(1);
        Limit largeLimit = Limit.of(10);

        Embedding embedding1 = Embedding.builder()
                .id(1L)
                .fileUuid(UUID.randomUUID())
                .fileName("doc1.pdf")
                .chunkText("Chunk 1")
                .vector(new float[768])
                .pageNumber(1)
                .userId(userId)
                .build();

        Embedding embedding2 = Embedding.builder()
                .id(2L)
                .fileUuid(UUID.randomUUID())
                .fileName("doc2.pdf")
                .chunkText("Chunk 2")
                .vector(new float[768])
                .pageNumber(2)
                .userId(userId)
                .build();

        when(embeddingRepository.findTopSimilar(eq(userId), eq(queryVector), eq(smallLimit)))
                .thenReturn(List.of(embedding1));

        when(embeddingRepository.findTopSimilar(eq(userId), eq(queryVector), eq(largeLimit)))
                .thenReturn(List.of(embedding1, embedding2));

        // When & Then: Small limit
        List<Embedding> result1 = embeddingRepository.findTopSimilar(userId, queryVector, smallLimit);
        assertThat(result1).hasSize(1);

        // When & Then: Large limit
        List<Embedding> result2 = embeddingRepository.findTopSimilar(userId, queryVector, largeLimit);
        assertThat(result2).hasSize(2);
    }
}