package com.github.vadimmiheev.vectordocs.notificationservice.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps mapping from userId -> set of WebSocket sessions.
 */
@Slf4j
@Component
public class NotificationSessionRegistry {

    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    public void register(String userId, WebSocketSession session) {
        sessionsByUser.compute(userId, (k, set) -> {
            if (set == null) set = Collections.newSetFromMap(new ConcurrentHashMap<>());
            set.add(session);
            return set;
        });
        log.debug("Registered WS session id={} for userId={}", session.getId(), userId);
        session.closeStatus().doOnTerminate(() -> unregister(userId, session)).subscribe();
    }

    public void unregister(String userId, WebSocketSession session) {
        Set<WebSocketSession> set = sessionsByUser.get(userId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) sessionsByUser.remove(userId);
            log.debug("Unregistered WS session id={} for userId={}", session.getId(), userId);
        }
    }

    public int sendToUser(String userId, String payload) {
        Set<WebSocketSession> set = sessionsByUser.get(userId);
        if (set == null || set.isEmpty()) {
            log.debug("No active WS sessions for userId={}", userId);
            return 0;
        }
        int count = 0;
        for (WebSocketSession s : set) {
            try {
                WebSocketMessage msg = s.textMessage(payload);
                Mono<Void> sendMono = s.send(Mono.just(msg)).then();
                sendMono.subscribe();
                count++;
            } catch (Exception e) {
                log.warn("Failed to send WS message to session id={} for userId={}: {}", s.getId(), userId, e.getMessage());
            }
        }
        return count;
    }
}
