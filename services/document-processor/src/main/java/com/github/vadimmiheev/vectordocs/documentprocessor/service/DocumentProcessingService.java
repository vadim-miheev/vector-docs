package com.github.vadimmiheev.vectordocs.documentprocessor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.documentprocessor.dto.DocumentProcessedEvent;
import com.github.vadimmiheev.vectordocs.documentprocessor.dto.DocumentUploadedEvent;
import com.github.vadimmiheev.vectordocs.documentprocessor.event.EmbeddingsGeneratedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
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
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.topics.documents-processed:documents.processed}")
    private String documentsProcessedTopic;

    public void process(DocumentUploadedEvent event) {
        try {
            byte[] bytes = downloadService.download(event.getDownloadUrl(), event.getUserId());
            ArrayList<String> pages = textExtractionService.extractText(bytes, event.getContentType(), event.getName());
            // Persist chunks
            int chunksCount = embeddingService.generateAndSaveEmbeddings(event, pages);

            log.info("Processed document id={} name='{}' size={} bytes. Pages processed: {}. Chunks: {}",
                    event.getId(), event.getName(), event.getSize(), pages.size(), chunksCount);
        } catch (Exception e) {
            log.error("Failed to process uploaded document id={} name='{}' due to: {}",
                    event.getId(), event.getName(), e.getMessage(), e);
        }
    }

    @EventListener
    private void publishDocumentProcessedEvent(EmbeddingsGeneratedEvent event) {
        try {
            int totalEmbeddings = Math.toIntExact(embeddingService.countTotalEmbeddings(event.fileUuid()));
            DocumentProcessedEvent processedEvent = new DocumentProcessedEvent( event.fileUuid(), event.userId(),
                    event.fileName(), totalEmbeddings);
            String payload = objectMapper.writeValueAsString(processedEvent);
            String key = event.fileUuid().toString();
            kafkaTemplate.send(documentsProcessedTopic, key, payload);
            log.info("Published event to topic '{}' for document id={} userId={} embeddingsCount={}",
                    documentsProcessedTopic, event.fileUuid(), event.userId(), totalEmbeddings);
        } catch (Exception ex) {
            log.error("Failed to publish '{}' event for document id={} userId={}",
                    documentsProcessedTopic, event.fileUuid(), event.userId(), ex);
        }
    }
}
