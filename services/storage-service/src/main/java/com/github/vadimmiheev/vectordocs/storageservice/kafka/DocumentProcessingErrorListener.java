package com.github.vadimmiheev.vectordocs.storageservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.storageservice.service.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingErrorListener {

    private final ObjectMapper objectMapper;
    private final DocumentStorageService documentStorageService;

    @KafkaListener(topics = "${app.topics.documents-processing-error:documents.processing.error}")
    public void onMessage(@Payload String message, ConsumerRecord<String, String> record) {
        try {
            log.info("Received processing error event from topic={} partition={} offset={}", record.topic(), record.partition(), record.offset());

            @SuppressWarnings("unchecked")
            HashMap<String, String> messageMap = objectMapper.readValue(message, HashMap.class);

            String docId = messageMap.get("id");
            String userId = messageMap.get("userId");

            if (docId == null || docId.isBlank()) {
                log.warn("Skipping processing error event without id. message={}, key={}", message, record.key());
                return;
            }

            if (userId == null || userId.isBlank()) {
                log.warn("Cannot delete document id={} on processing error because userId is unknown and document not found", docId);
                return;
            }

            long deleted = documentStorageService.delete(userId, docId);
            if (deleted > 0) {
                log.info("Deleted document id={} userId={} due to processing error", docId, userId);
            } else {
                log.warn("Document id={} userId={} was not deleted (possibly already removed)", docId, userId);
            }
        } catch (Exception e) {
            log.error("Failed to handle processing error event: {} due to: {}", message, e.getMessage(), e);
        }
    }
}
