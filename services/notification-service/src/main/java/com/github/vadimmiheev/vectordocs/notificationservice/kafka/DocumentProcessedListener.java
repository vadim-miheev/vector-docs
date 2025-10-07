package com.github.vadimmiheev.vectordocs.notificationservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;

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
public class DocumentProcessedListener {

    private final ObjectMapper objectMapper;
    private final NotificationSessionRegistry sessionRegistry;

    @KafkaListener(topics = "${app.topics.documents-processed:documents.processed}")
    public void onMessage(@Payload String message, ConsumerRecord<String, String> record) {
        try {
            log.info("[notifications] Received documents.processed from topic={} partition={} offset={}", record.topic(), record.partition(), record.offset());

            @SuppressWarnings("unchecked")
            HashMap<String, String> messageMap = objectMapper.readValue(message, HashMap.class);

            if (!messageMap.containsKey("userId")) {
                log.warn("documents.processed message has no userId, skipping WS push. key={}, message={}", record.key(), message);
                return;
            }

            String userId = String.valueOf(messageMap.get("userId"));

            messageMap.put("event", "documents.processed");
            int sent = sessionRegistry.sendToUser(userId, objectMapper.writeValueAsString(messageMap));
            if (sent > 0) {
                log.info("Forwarded documents.processed to {} WS session(s) for userId={}", sent, userId);
            } else {
                log.debug("No active WS sessions to deliver documents.processed for userId={}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to process documents.processed message due to: {}. Message={} ", e.getMessage(), message, e);
        }
    }
}
