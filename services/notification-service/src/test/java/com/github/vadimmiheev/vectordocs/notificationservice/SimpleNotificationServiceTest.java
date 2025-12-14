package com.github.vadimmiheev.vectordocs.notificationservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.kafka.consumer.group-id=test-group",
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
class SimpleNotificationServiceTest {

    @Test
    void contextLoads() {
        // Simple test to verify Spring context loads
        assertTrue(true, "Context should load successfully");
    }

    @Test
    void notificationServiceStarts() {
        // Basic test to verify the service can start
        assertTrue(true, "Notification service should be testable");
    }
}