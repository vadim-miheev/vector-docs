package com.github.vadimmiheev.vectordocs.storageservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.storageservice.dto.DocumentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class DocumentUploadedEventListener {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.topics.documents-uploaded:documents.uploaded}")
    private String documentsUploadedTopic;

    public DocumentUploadedEventListener(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentUploaded(DocumentResponse document) {
        try {
            kafkaTemplate.send(documentsUploadedTopic, document.getId(), objectMapper.writeValueAsString(document));
            log.info("Published event to topic '{}' for document upload id={} userId={}", documentsUploadedTopic, document.getId(), document.getUserId());
        } catch (Exception ex) {
            log.error("Failed to publish '{}' event for id={} userId={}", documentsUploadedTopic, document.getId(), document.getUserId(), ex);
        }
    }
}
