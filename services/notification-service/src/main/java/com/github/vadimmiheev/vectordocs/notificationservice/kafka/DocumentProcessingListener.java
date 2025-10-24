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

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingListener {

    private final ObjectMapper objectMapper;
    private final NotificationSessionRegistry sessionRegistry;

    @KafkaListener(topics = "${app.topics.documents-processing:documents.processing}")
    public void onMessage(@Payload String message, ConsumerRecord<String, String> record) {
        try {
            log.debug("[notifications] Received documents.processing from topic={} partition={} offset={}", record.topic(), record.partition(), record.offset());

            @SuppressWarnings("unchecked")
            HashMap<String, String> messageMap = objectMapper.readValue(message, HashMap.class);

            if (!messageMap.containsKey("userId")) {
                log.warn("documents.processing message has no userId, skipping WS push. key={}, message={}", record.key(), message);
                return;
            }

            String userId = String.valueOf(messageMap.get("userId"));

            messageMap.put("event", "documents.processing");
            int sent = sessionRegistry.sendToUser(userId, objectMapper.writeValueAsString(messageMap));
            if (sent > 0) {
                log.debug("Forwarded documents.processing to {} WS session(s) for userId={}", sent, userId);
            } else {
                log.debug("No active WS sessions to deliver documents.processing for userId={}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to process documents.processing message due to: {}. Message={} ", e.getMessage(), message, e);
        }
    }
}