package com.github.vadimmiheev.vectordocs.documentprocessor.service;

import com.github.vadimmiheev.vectordocs.documentprocessor.dto.DocumentUploadedEvent;
import com.github.vadimmiheev.vectordocs.documentprocessor.entity.Embedding;
import com.github.vadimmiheev.vectordocs.documentprocessor.repository.EmbeddingRepository;
import com.github.vadimmiheev.vectordocs.documentprocessor.util.TextChunker;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final OpenAiEmbeddingModel embeddingModel;
    private final EmbeddingRepository embeddingRepository;

    @Value("${app.embedding.chunk-size:600}")
    private int chunkSize;

    @Value("${app.embedding.chunk-overlap:100}")
    private int chunkOverlap;

    @Value("${app.embedding.batch-size:32}")
    private int batchSize;

    @Value("${app.embedding.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${app.embedding.retry.backoff-ms:500}")
    private long retryBackoffMs;

    public int generateAndSaveEmbeddings(DocumentUploadedEvent event, ArrayList<String> pages) {
        try {
            UUID fileUuid = event.getId();
            String userId = event.getUserId();
            Instant createdAt = Instant.now();

            List<Integer> pageOffsets = new ArrayList<>(); // The beginning of each page in the general text
            StringBuilder wholeText = new StringBuilder();
            for (String page : pages) {
                pageOffsets.add(wholeText.length());
                wholeText.append(page);
            }

            List<String> chunks = TextChunker.split(wholeText.toString(), chunkSize, chunkOverlap);
            if (chunks.isEmpty()) {
                log.warn("No text chunks produced for file id={} name='{}'", event.getId(), event.getName());
                return 0;
            }

            List<Integer> pageNumbers = new ArrayList<>();
            int[] offsetsArray = pageOffsets.stream().mapToInt(i -> i).toArray();

            int currentPage = 0;
            for (String chunk : chunks) {
                int chunkStart = wholeText.indexOf(chunk);
                while (currentPage + 1 < offsetsArray.length && chunkStart >= offsetsArray[currentPage + 1]) {
                    currentPage++;
                }
                pageNumbers.add(currentPage + 1);
            }

            // Convert chunks to segments for LangChain4j
            List<TextSegment> segments = new ArrayList<>(chunks.size());
            for (String chunk : chunks) {
                segments.add(TextSegment.from(chunk));
            }

            // Embedding generation with batches with retrays and staged saving
            int totalSaved = 0;
            for (int start = 0; start < segments.size(); start += Math.max(1, batchSize)) {
                int end = Math.min(start + Math.max(1, batchSize), segments.size());
                List<TextSegment> batch = segments.subList(start, end);

                List<dev.langchain4j.data.embedding.Embedding> vectors = embedBatchWithRetry(batch);

                List<Embedding> entities = new ArrayList<>(batch.size());
                for (int i = 0; i < batch.size(); i++) {
                    int globalIndex = start + i;
                    Embedding entity = Embedding.builder()
                            .fileUuid(fileUuid)
                            .fileName(event.getName())
                            .userId(userId)
                            .createdAt(createdAt)
                            .chunkText(chunks.get(globalIndex))
                            .vector(vectors.get(i).vector())
                            .pageNumber(pageNumbers.get(globalIndex))
                            .build();
                    entities.add(entity);
                }

                embeddingRepository.saveAll(entities);
                totalSaved += entities.size();
                log.info("Saved {} embeddings so far for document id={} (batch {}-{})", totalSaved, fileUuid, start, end - 1);
            }

            log.info("Saved {} embeddings for document id={} userId={}", totalSaved, fileUuid, userId);
            return totalSaved;
        } catch (Exception e) {
            log.error("Failed generating/saving embeddings for id={} due to: {}", event.getId(), e.getMessage(), e);
        }
        return 0;
    }

    private List<dev.langchain4j.data.embedding.Embedding> embedBatchWithRetry(List<TextSegment> batch) throws InterruptedException {
        int attempt = 0;
        long backoff = Math.max(0L, retryBackoffMs);
        while (true) {
            try {
                return embeddingModel.embedAll(batch).content();
            } catch (Exception ex) {
                attempt++;
                if (attempt >= Math.max(1, maxRetryAttempts)) {
                    throw ex;
                }
                long sleepMs = backoff * attempt; // linear-exponential growth
                log.warn("Embedding batch failed (attempt {}/{}). Will retry in {} ms. Reason: {}",
                        attempt, maxRetryAttempts, sleepMs, ex.getMessage());
                Thread.sleep(sleepMs);
            }
        }
    }

    public void deleteEmbeddingsByDocumentId(UUID documentId) {
        try {
            embeddingRepository.deleteByFileUuid(documentId);
            log.info("Deleted embeddings for document id={}", documentId);
        } catch (Exception e) {
            log.error("Failed to delete embeddings for document id={} due to: {}", documentId, e.getMessage(), e);
        }
    }
}
