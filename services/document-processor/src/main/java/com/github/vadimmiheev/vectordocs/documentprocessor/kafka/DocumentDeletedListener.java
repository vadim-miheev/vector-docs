package com.github.vadimmiheev.vectordocs.documentprocessor.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.documentprocessor.dto.DocumentDeletedEvent;
import com.github.vadimmiheev.vectordocs.documentprocessor.service.EmbeddingService;
import com.github.vadimmiheev.vectordocs.documentprocessor.util.DocumentsStatusStore;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
@Slf4j
public class DocumentDeletedListener {

    private final ObjectMapper objectMapper;
    private final EmbeddingService embeddingService;

    @KafkaListener(topics = "${app.topics.documents-deleted:documents.deleted}")
    @Transactional
    public void onMessage(@Payload String message, ConsumerRecord<String, String> record) {
        try {
            log.info("Received delete message from topic={} partition={} offset={}", record.topic(), record.partition(), record.offset());
            DocumentDeletedEvent event = objectMapper.readValue(message, DocumentDeletedEvent.class);
            log.debug("Parsed delete event: {}", event);
            DocumentsStatusStore.cancel(event.getDocumentId().toString());
            embeddingService.deleteEmbeddingsByDocumentId(event.getDocumentId());
        } catch (Exception e) {
            log.error("Failed to handle delete message: {} due to: {}", message, e.getMessage(), e);
        }
    }
}
