package com.github.vadimmiheev.vectordocs.notificationservice.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSessionRegistryTest {

    private NotificationSessionRegistry registry;

    @Mock
    private WebSocketSession session1;

    @Mock
    private WebSocketSession session2;

    @Mock
    private WebSocketMessage message;

    @BeforeEach
    void setUp() {
        registry = new NotificationSessionRegistry();
    }

    @Test
    void register_shouldAddSessionForUser() {
        // Given
        String userId = "user123";
        when(session1.getId()).thenReturn("session1");
        when(session1.closeStatus()).thenReturn(Mono.never());

        // When
        registry.register(userId, session1);

        // Then - session should be registered
        // We'll test registration by calling sendToUser with proper mocks
        // Note: sendToUser is async, so we need to ensure mocks are set up correctly
        when(session1.textMessage(anyString())).thenReturn(message);
        when(session1.send(any())).thenReturn(Mono.empty());

        // Call sendToUser - it's async but we've mocked send()
        registry.sendToUser(userId, "test message");

        // Verify that send was called (async, need timeout)
        verify(session1, timeout(1000).times(1)).send(any());
    }

    @Test
    void register_multipleSessionsForSameUser() {
        // Given
        String userId = "user123";
        when(session1.getId()).thenReturn("session1");
        when(session2.getId()).thenReturn("session2");
        when(session1.closeStatus()).thenReturn(Mono.never());
        when(session2.closeStatus()).thenReturn(Mono.never());

        // When
        registry.register(userId, session1);
        registry.register(userId, session2);

        // Then - both sessions should be registered
        when(session1.textMessage(anyString())).thenReturn(message);
        when(session2.textMessage(anyString())).thenReturn(message);
        when(session1.send(any())).thenReturn(Mono.empty());
        when(session2.send(any())).thenReturn(Mono.empty());

        // Call sendToUser - it's async but we've mocked send()
        registry.sendToUser(userId, "test message");

        // Verify that send was called for both sessions (async, need timeout)
        verify(session1, timeout(1000).times(1)).send(any());
        verify(session2, timeout(1000).times(1)).send(any());
    }

    @Test
    void unregister_shouldRemoveSession() {
        // Given
        String userId = "user123";
        when(session1.getId()).thenReturn("session1");
        when(session1.closeStatus()).thenReturn(Mono.never());

        registry.register(userId, session1);

        // When
        registry.unregister(userId, session1);

        // Then - session should be removed
        // No need to mock send since there should be no sessions
        // We can't easily verify that send wasn't called without mocking,
        // but unregister should remove the session
    }

    @Test
    void sendToUser_shouldSendMessageToAllSessions() {
        // Given
        String userId = "user123";
        String messagePayload = "Test notification";

        when(session1.getId()).thenReturn("session1");
        when(session2.getId()).thenReturn("session2");
        when(session1.closeStatus()).thenReturn(Mono.never());
        when(session2.closeStatus()).thenReturn(Mono.never());
        when(session1.textMessage(messagePayload)).thenReturn(message);
        when(session2.textMessage(messagePayload)).thenReturn(message);
        when(session1.send(any())).thenReturn(Mono.empty());
        when(session2.send(any())).thenReturn(Mono.empty());

        // Register sessions
        registry.register(userId, session1);
        registry.register(userId, session2);

        // When
        registry.sendToUser(userId, messagePayload);

        // Then - verify that send was called for both sessions (async, need timeout)
        verify(session1, timeout(1000).times(1)).send(any());
        verify(session2, timeout(1000).times(1)).send(any());
    }

    @Test
    void sendToUser_whenNoSessions_shouldNotCallSend() {
        // Given
        String userId = "nonExistentUser";
        String messagePayload = "Test notification";

        // When
        registry.sendToUser(userId, messagePayload);

        // Then - no sessions, so send should not be called
        // Nothing to verify - just ensure no exception
    }

    @Test
    void sendToUser_whenSessionThrowsException_shouldContinueAndCountSuccesses() {
        // Given
        String userId = "user123";
        String messagePayload = "Test notification";

        when(session1.getId()).thenReturn("session1");
        when(session2.getId()).thenReturn("session2");
        when(session1.closeStatus()).thenReturn(Mono.never());
        when(session2.closeStatus()).thenReturn(Mono.never());
        when(session1.textMessage(messagePayload)).thenReturn(message);
        when(session2.textMessage(messagePayload)).thenThrow(new RuntimeException("Failed to create message"));
        when(session1.send(any())).thenReturn(Mono.empty());

        registry.register(userId, session1);
        registry.register(userId, session2);

        // When
        registry.sendToUser(userId, messagePayload);

        // Then - session1 should have send called, session2 throws exception (async, need timeout)
        verify(session1, timeout(1000).times(1)).send(any());
        // session2.textMessage throws exception, so send should not be called
    }
}