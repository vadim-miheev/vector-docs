package com.github.vadimmiheev.vectordocs.storageservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class DocumentDeletedEventListener {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.topics.documents-deleted:documents.deleted}")
    private String documentsDeletedTopic;

    public DocumentDeletedEventListener(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentDeleted(DocumentDeletedEvent event) {
        try {
            kafkaTemplate.send(documentsDeletedTopic, event.documentId(), objectMapper.writeValueAsString(event));
            log.info("Published event to topic '{}' for document deletion id={} userId={}", documentsDeletedTopic, event.documentId(), event.userId());
        } catch (Exception ex) {
            log.error("Failed to publish '{}' event for id={} userId={}", documentsDeletedTopic, event.documentId(), event.userId(), ex);
        }
    }
}
