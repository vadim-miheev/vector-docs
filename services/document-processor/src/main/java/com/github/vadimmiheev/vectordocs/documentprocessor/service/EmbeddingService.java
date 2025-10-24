package com.github.vadimmiheev.vectordocs.documentprocessor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.documentprocessor.dto.DocumentProcessingEvent;
import com.github.vadimmiheev.vectordocs.documentprocessor.dto.DocumentUploadedEvent;
import com.github.vadimmiheev.vectordocs.documentprocessor.entity.Embedding;
import com.github.vadimmiheev.vectordocs.documentprocessor.event.EmbeddingsGeneratedEvent;
import com.github.vadimmiheev.vectordocs.documentprocessor.repository.EmbeddingRepository;
import com.github.vadimmiheev.vectordocs.documentprocessor.util.TextChunker;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final OpenAiEmbeddingModel embeddingModel;
    private final EmbeddingRepository embeddingRepository;
    private final ApplicationEventPublisher publisher;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.embedding.chunk-size:600}")
    private int chunkSize;

    @Value("${app.embedding.chunk-overlap:100}")
    private int chunkOverlap;

    @Value("${app.embedding.generator-batch:100}")
    private int generatorBatchSize;

    @Value("${app.topics.documents-processing:documents.processing}")
    private String documentsProcessingTopic;

    private final ReentrantLock embeddingGeneratorLock = new ReentrantLock(true); // fair=true

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

            // Persist chunks only (vectors are null for now)
            List<Embedding> entities = new ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                Embedding entity = Embedding.builder()
                        .fileUuid(fileUuid)
                        .fileName(event.getName())
                        .userId(userId)
                        .createdAt(createdAt)
                        .chunkText(chunks.get(i))
                        .pageNumber(pageNumbers.get(i))
                        .build();
                entities.add(entity);
            }

            embeddingRepository.saveAll(entities);
            log.info("Saved {} text chunks for document id={} userId={}", entities.size(), fileUuid, userId);

            // Start embeddings' generation in a separate thread
            new Thread(() -> {
                try {
                    while (true) {
                        long remaining = processPendingEmbeddingsForDocument(fileUuid, event.getName(), userId);
                        if (remaining == 0) break;
                    }
                } catch (Exception ex) {
                    log.error("Background embeddings generation failed for id={} userId={} due to: {}", fileUuid, userId, ex.getMessage(), ex);
                }
            }, "embeddings-generator-" + fileUuid).start();

            return entities.size();
        } catch (Exception e) {
            log.error("Failed generating/saving embeddings for id={} due to: {}", event.getId(), e.getMessage(), e);
        }
        return 0;
    }

    public void deleteEmbeddingsByDocumentId(UUID documentId) {
        try {
            embeddingRepository.deleteByFileUuid(documentId);
            log.info("Deleted embeddings for document id={}", documentId);
        } catch (Exception e) {
            log.error("Failed to delete embeddings for document id={} due to: {}", documentId, e.getMessage(), e);
        }
    }

    public long processPendingEmbeddingsForDocument(UUID fileUuid, String fileName, String userId) {
        embeddingGeneratorLock.lock();

        try {
            List<Embedding> pending = embeddingRepository.findByFileUuidAndVectorGenerated(fileUuid, false, Limit.of(generatorBatchSize));
            if (pending == null || pending.isEmpty()) {
                log.info("No pending embeddings for document id={}", fileUuid);
                return 0;
            }

            // Prepare segments
            List<TextSegment> segments = new ArrayList<>(pending.size());
            for (Embedding e : pending) {
                segments.add(TextSegment.from(e.getChunkText()));
            }
            // Generate vectors in batch
            List<dev.langchain4j.data.embedding.Embedding> vectors = embeddingModel.embedAll(segments).content();
            for (int i = 0; i < pending.size(); i++) {
                Embedding embedding = pending.get(i);
                embedding.setVector(vectors.get(i).vector());
                embedding.setVectorGenerated(true);
            }
            embeddingRepository.saveAll(pending);

            long remaining = embeddingRepository.countByFileUuidAndVectorGeneratedFalse(fileUuid);
            log.info("Generated {} vectors for document id={} userId={}, remaining {} chunks",
                    pending.size(), fileUuid, userId, remaining);

            if (remaining == 0) {
                // All embeddings generated -> publish event
                publisher.publishEvent(new EmbeddingsGeneratedEvent(fileUuid, userId, fileName));
            } else {
                // Calculate progress percentage and publish processing event
                long totalEmbeddings = embeddingRepository.countByFileUuid(fileUuid);
                if (totalEmbeddings > 0) {
                    int progressPercentage = (int) Math.round((double) (totalEmbeddings - remaining) / totalEmbeddings * 100);

                    try {
                        DocumentProcessingEvent processingEvent = new DocumentProcessingEvent(fileUuid, userId, fileName, progressPercentage);
                        String payload = objectMapper.writeValueAsString(processingEvent);
                        String key = fileUuid.toString();
                        kafkaTemplate.send(documentsProcessingTopic, key, payload);
                        log.debug("Published processing progress event to topic '{}' for document id={} userId={} progress={}%",
                                documentsProcessingTopic, fileUuid, userId, progressPercentage);
                    } catch (Exception ex) {
                        log.error("Failed to publish processing progress event for document id={} userId={}",
                                fileUuid, userId, ex);
                    }
                }
            }
            return remaining;
        } catch (Exception e) {
            log.error("Failed to generate embeddings for document id={} due to: {}", fileUuid, e.getMessage(), e);
        } finally {
            embeddingGeneratorLock.unlock();
        }

        return 0;
    }

    public long countTotalEmbeddings(UUID fileUuid) {
        return embeddingRepository.countByFileUuid(fileUuid);
    }

    @Scheduled(fixedDelayString = "${app.embedding.scheduler.delay-ms:60000}")
    public void processPendingEmbeddingsScheduled() {
        try {
            List<Embedding> pending = embeddingRepository.findByVectorGeneratedFalse();
            if (pending == null || pending.isEmpty()) {
                return;
            }
            // Group by document and process each document
            Map<UUID, Embedding> anyPerDoc = new HashMap<>();
            for (Embedding e : pending) {
                anyPerDoc.putIfAbsent(e.getFileUuid(), e);
            }
            for (Map.Entry<UUID, Embedding> entry : anyPerDoc.entrySet()) {
                UUID fileUuid = entry.getKey();
                Embedding e = entry.getValue();
                while (true) {
                    long remaining = processPendingEmbeddingsForDocument(fileUuid, e.getFileName(), e.getUserId());
                    if (remaining == 0) break;
                }
            }
        } catch (Exception e) {
            log.error("Scheduled processing of pending embeddings failed: {}", e.getMessage(), e);
        }
    }
}
