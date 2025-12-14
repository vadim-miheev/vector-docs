package com.github.vadimmiheev.vectordocs.notificationservice.ws;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationsWebSocketHandlerTest {

    @Mock
    private NotificationSessionRegistry sessionRegistry;

    @Mock
    private WebSocketSession session;

    @Mock
    private org.springframework.web.reactive.socket.HandshakeInfo handshakeInfo;

    @Test
    void handle_withValidUserId_shouldRegisterSession() {
        // Given
        NotificationsWebSocketHandler handler = new NotificationsWebSocketHandler(sessionRegistry);
        String userId = "user123";

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", userId);

        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getHeaders()).thenReturn(headers);
        lenient().when(session.getId()).thenReturn("session123");
        lenient().when(session.closeStatus()).thenReturn(Mono.never());
        lenient().when(session.receive()).thenReturn(Flux.empty());

        // When
        Mono<Void> result = handler.handle(session);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(sessionRegistry, times(1)).register(userId, session);
        verify(session, times(1)).receive();
    }

    @Test
    void handle_withoutUserId_shouldNotRegisterSession() {
        // Given
        NotificationsWebSocketHandler handler = new NotificationsWebSocketHandler(sessionRegistry);

        HttpHeaders headers = new HttpHeaders();
        // No X-User-Id header

        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getHeaders()).thenReturn(headers);
        lenient().when(session.getId()).thenReturn("session123");
        lenient().when(session.closeStatus()).thenReturn(Mono.never());
        lenient().when(session.receive()).thenReturn(Flux.empty());

        // When
        Mono<Void> result = handler.handle(session);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(sessionRegistry, never()).register(any(), any());
        verify(session, times(1)).receive();
    }

    @Test
    void handle_withBlankUserId_shouldNotRegisterSession() {
        // Given
        NotificationsWebSocketHandler handler = new NotificationsWebSocketHandler(sessionRegistry);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "   ");

        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getHeaders()).thenReturn(headers);
        lenient().when(session.getId()).thenReturn("session123");
        lenient().when(session.closeStatus()).thenReturn(Mono.never());
        lenient().when(session.receive()).thenReturn(Flux.empty());

        // When
        Mono<Void> result = handler.handle(session);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(sessionRegistry, never()).register(any(), any());
        verify(session, times(1)).receive();
    }

    @Test
    void handle_withNullHeaders_shouldNotRegisterSession() {
        // Given
        NotificationsWebSocketHandler handler = new NotificationsWebSocketHandler(sessionRegistry);

        HttpHeaders headers = null;

        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getHeaders()).thenReturn(headers);
        lenient().when(session.getId()).thenReturn("session123");
        lenient().when(session.closeStatus()).thenReturn(Mono.never());
        lenient().when(session.receive()).thenReturn(Flux.empty());

        // When
        Mono<Void> result = handler.handle(session);

        // Then - headers are null, userId is null, should not register
        StepVerifier.create(result)
                .verifyComplete();

        verify(sessionRegistry, never()).register(any(), any());
        verify(session, times(1)).receive();
    }
}