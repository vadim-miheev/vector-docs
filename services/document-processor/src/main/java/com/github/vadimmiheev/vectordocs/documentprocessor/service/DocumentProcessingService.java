package com.github.vadimmiheev.vectordocs.documentprocessor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.documentprocessor.dto.DocumentUploadedEvent;
import com.github.vadimmiheev.vectordocs.documentprocessor.dto.DocumentProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingService {

    private final DownloadService downloadService;
    private final TextExtractionService textExtractionService;
    private final EmbeddingService embeddingService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.topics.documents-processed:documents.processed}")
    private String documentsProcessedTopic;

    public void process(DocumentUploadedEvent event) {
        try {
            byte[] bytes = downloadService.download(event.getDownloadUrl(), event.getUserId());
            ArrayList<String> pages = textExtractionService.extractText(bytes, event.getContentType(), event.getName());
            // Generate and persist embeddings
            int embeddingsCount = embeddingService.generateAndSaveEmbeddings(event, pages);

            // Publish documents.processed event (best-effort)
            try {
                DocumentProcessedEvent processedEvent = new DocumentProcessedEvent(
                        event.getId(),
                        event.getUserId(),
                        event.getName(),
                        embeddingsCount
                );
                String payload = objectMapper.writeValueAsString(processedEvent);
                String key = event.getId().toString();
                kafkaTemplate.send(documentsProcessedTopic, key, payload);
                log.info("Published event to topic '{}' for document id={} userId={} embeddingsCount={}",
                        documentsProcessedTopic, event.getId(), event.getUserId(), embeddingsCount);
            } catch (Exception ex) {
                log.error("Failed to publish '{}' event for document id={} userId={}",
                        documentsProcessedTopic, event.getId(), event.getUserId(), ex);
            }

            log.info("Processed document id={} name='{}' size={} bytes. Pages processed: {}.",
                    event.getId(), event.getName(), event.getSize(), pages.size());
        } catch (Exception e) {
            log.error("Failed to process uploaded document id={} name='{}' due to: {}",
                    event.getId(), event.getName(), e.getMessage(), e);
        }
    }
}
