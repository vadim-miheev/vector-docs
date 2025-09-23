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
import java.util.stream.Collectors;

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

    public void generateAndSaveEmbeddings(DocumentUploadedEvent event, String text) {
        try {
            UUID fileUuid = event.getId();
            String userId = event.getUserId();
            Instant createdAt = Instant.now();

            List<String> chunks = TextChunker.split(text, chunkSize, chunkOverlap);
            if (chunks.isEmpty()) {
                log.warn("No text chunks produced for file id={} name='{}'", event.getId(), event.getName());
                return;
            }

            // Convert chunks to segments for LangChain4j
            List<TextSegment> segments = new ArrayList<>(chunks.size());
            for (String chunk : chunks) {
                segments.add(TextSegment.from(chunk));
            }

            // Generate embeddings in batch
            List<dev.langchain4j.data.embedding.Embedding> vectors = embeddingModel.embedAll(segments).content();

            List<Embedding> entities = new ArrayList<>(segments.size());
            for (int i = 0; i < segments.size(); i++) {
                String vecStr = vectorToString(vectors.get(i));
                Embedding entity = Embedding.builder()
                        .fileUuid(fileUuid)
                        .userId(userId)
                        .createdAt(createdAt)
                        .chunkText(chunks.get(i))
                        .vector(vecStr)
                        .pageNumber(null)
                        .build();
                entities.add(entity);
            }

            embeddingRepository.saveAll(entities);
            log.info("Saved {} embeddings for document id={} userId={}", entities.size(), fileUuid, userId);
        } catch (Exception e) {
            log.error("Failed generating/saving embeddings for id={} due to: {}", event.getId(), e.getMessage(), e);
        }
    }

    private String vectorToString(dev.langchain4j.data.embedding.Embedding embedding) {
        try {
            // Attempt 1: vectorAsList() API (floats)
            List<Float> list = embedding.vectorAsList();
            return list.stream().map(f -> Float.toString(f)).collect(Collectors.joining(","));
        } catch (Throwable ignore) {
            try {
                // Attempt 2: vector() returns float[]
                float[] arr = embedding.vector();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arr.length; i++) {
                    if (i > 0) sb.append(',');
                    sb.append(arr[i]);
                }
                return sb.toString();
            } catch (Throwable ex) {
                // Fallback to toString()
                return String.valueOf(embedding);
            }
        }
    }
}
