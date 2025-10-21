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

            // Generate embeddings in batch
            List<dev.langchain4j.data.embedding.Embedding> vectors = embeddingModel.embedAll(segments).content();

            List<Embedding> entities = new ArrayList<>(segments.size());
            for (int i = 0; i < segments.size(); i++) {
                Embedding entity = Embedding.builder()
                        .fileUuid(fileUuid)
                        .fileName(event.getName())
                        .userId(userId)
                        .createdAt(createdAt)
                        .chunkText(chunks.get(i))
                        .vector(vectors.get(i).vector())
                        .pageNumber(pageNumbers.get(i))
                        .build();
                entities.add(entity);
            }

            embeddingRepository.saveAll(entities);
            log.info("Saved {} embeddings for document id={} userId={}", entities.size(), fileUuid, userId);
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
}
