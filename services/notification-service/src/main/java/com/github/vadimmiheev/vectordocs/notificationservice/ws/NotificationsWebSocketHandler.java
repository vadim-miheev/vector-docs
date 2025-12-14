package com.github.vadimmiheev.vectordocs.notificationservice.ws;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationsWebSocketHandler implements WebSocketHandler {

    private final NotificationSessionRegistry sessionRegistry;

    @Override
    public @NonNull Mono<Void> handle(@NonNull WebSocketSession session) {
        var headers = session.getHandshakeInfo().getHeaders();
        String userId = headers != null ? headers.getFirst("X-User-Id") : null;
        if (userId == null || userId.isBlank()) {
            log.warn("WS connection without userId, session id={}", session.getId());
            // Keep the connection open but we won't be able to route user-specific notifications
            return session.receive().then();
        }
        sessionRegistry.register(userId, session);
        // Echo any client pings back optionally, or just consume and ignore
        return session.receive().then();
    }
}
