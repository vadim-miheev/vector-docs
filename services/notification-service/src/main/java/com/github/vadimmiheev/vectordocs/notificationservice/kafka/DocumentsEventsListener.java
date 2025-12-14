package com.github.vadimmiheev.vectordocs.notificationservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.notificationservice.ws.NotificationSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * Redirects Documents events to the UI
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentsEventsListener {

    private final ObjectMapper objectMapper;
    private final NotificationSessionRegistry sessionRegistry;

    @KafkaListener(topics = {
            "${app.topics.documents-uploaded:documents.uploaded}",
            "${app.topics.documents-processed:documents.processed}",
            "${app.topics.documents-processing:documents.processing}",
            "${app.topics.documents-processing-error:documents.processing.error}"
    })
    public void onMessage(@Payload String message, ConsumerRecord<String, String> record) {
        try {
            log.debug("[notifications] Received from topic={} partition={} offset={}", record.topic(), record.partition(), record.offset());

            @SuppressWarnings("unchecked")
            HashMap<String, String> messageMap = objectMapper.readValue(message, HashMap.class);

            if (!messageMap.containsKey("userId") || messageMap.get("userId") == null) {
                log.warn("{} message has no userId or userId is null, skipping WS push. key={}, message={}", record.topic(), record.key(), message);
                return;
            }

            String userId = String.valueOf(messageMap.get("userId"));

            messageMap.put("event", record.topic());
            int sent = sessionRegistry.sendToUser(userId, objectMapper.writeValueAsString(messageMap));
            if (sent > 0) {
                log.debug("Forwarded {} to {} WS session(s) for userId={}", record.topic(), sent, userId);
            } else {
                log.debug("No active WS sessions to deliver {} for userId={}", record.topic(), userId);
            }
        } catch (Exception e) {
            log.error("Failed to process {} message due to: {}. Message={} ", record.topic(), e.getMessage(), message, e);
        }
    }
}