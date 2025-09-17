package com.github.vadimmiheev.vectordocs.documentprocessor.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.documentprocessor.dto.DocumentUploadedEvent;
import com.github.vadimmiheev.vectordocs.documentprocessor.service.DocumentProcessingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class DocumentUploadedListener {

    private final ObjectMapper objectMapper;
    private final DocumentProcessingService processingService;

    @KafkaListener(topics = "${app.topics.documents-uploaded:documents.uploaded}")
    public void onMessage(@Payload String message, ConsumerRecord<String, String> record) {
        try {
            log.info("Received message from topic={} partition={} offset={}", record.topic(), record.partition(), record.offset());
            DocumentUploadedEvent event = objectMapper.readValue(message, DocumentUploadedEvent.class);
            log.debug("Parsed event: {}", event);
            processingService.process(event);
        } catch (Exception e) {
            log.error("Failed to handle message: {} due to: {}", message, e.getMessage(), e);
        }
    }
}
