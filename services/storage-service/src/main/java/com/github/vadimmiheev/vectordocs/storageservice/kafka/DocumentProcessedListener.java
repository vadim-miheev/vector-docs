package com.github.vadimmiheev.vectordocs.storageservice.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.storageservice.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessedListener {

    private final ObjectMapper objectMapper;
    private final DocumentRepository documentRepository;

    @KafkaListener(topics = "${app.topics.documents-processed:documents.processed}")
    public void onMessage(@Payload String message, ConsumerRecord<String, String> record) {
        try {
            log.info("Received processed event from topic={} partition={} offset={}", record.topic(), record.partition(), record.offset());
            String id = null;
            try {
                JsonNode node = objectMapper.readTree(message);
                if (node.hasNonNull("id")) {
                    id = node.get("id").asText();
                }
            } catch (Exception e) {
                log.warn("Failed to parse message as JSON, will try key. Message: {}", message);
            }
            if (id == null || id.isBlank()) {
                id = record.key();
            }
            if (id == null || id.isBlank()) {
                log.warn("Skipping processed event without id. message={}, key={}", message, record.key());
                return;
            }

            final String docId = id;
            documentRepository.findById(docId).ifPresentOrElse(doc -> {
                if (!doc.isProcessed()) {
                    doc.setProcessed(true);
                    documentRepository.save(doc);
                    log.info("Marked document id={} as processed", docId);
                } else {
                    log.debug("Document id={} is already marked as processed", docId);
                }
            }, () -> log.warn("Document with id={} not found to mark as processed", docId));
        } catch (Exception e) {
            log.error("Failed to handle processed event: {} due to: {}", message, e.getMessage(), e);
        }
    }
}
