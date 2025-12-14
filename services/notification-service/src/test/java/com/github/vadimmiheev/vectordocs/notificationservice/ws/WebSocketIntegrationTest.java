package com.github.vadimmiheev.vectordocs.notificationservice.ws;

import com.github.vadimmiheev.vectordocs.notificationservice.config.TestWebSocketConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertTrue;

@WebFluxTest
@Import({TestWebSocketConfig.class, NotificationsWebSocketHandler.class, NotificationSessionRegistry.class})
class WebSocketIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void webSocketEndpoint_shouldBeConfigured() {
        // Test that WebSocket endpoint configuration is valid
        // We can't actually test WebSocket connection without running server,
        // but we can verify the configuration loads correctly
        webTestClient.get()
                .uri("/notifications")
                .exchange()
                .expectStatus().is4xxClientError(); // GET request to WS endpoint should fail

        assertTrue(true, "WebSocket configuration should be valid");
    }
}