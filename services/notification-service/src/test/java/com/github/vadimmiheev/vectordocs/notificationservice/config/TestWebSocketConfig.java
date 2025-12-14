package com.github.vadimmiheev.vectordocs.notificationservice.config;

import com.github.vadimmiheev.vectordocs.notificationservice.ws.NotificationsWebSocketHandler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

@TestConfiguration
public class TestWebSocketConfig {

    @Bean
    public SimpleUrlHandlerMapping webSocketMapping(NotificationsWebSocketHandler wsHandler) {
        Map<String, Object> map = new HashMap<>();
        map.put("/notifications", wsHandler);
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(10);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}