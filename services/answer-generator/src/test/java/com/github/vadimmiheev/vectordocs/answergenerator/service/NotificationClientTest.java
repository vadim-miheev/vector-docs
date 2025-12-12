package com.github.vadimmiheev.vectordocs.answergenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationClientTest {

    @Mock
    private RSocketRequester.Builder rSocketRequesterBuilder;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RSocketRequester rSocketRequester;

    @Captor
    private ArgumentCaptor<String> metadataCaptor;

    @Captor
    private ArgumentCaptor<MimeType> mimeTypeCaptor;

    @Captor
    private ArgumentCaptor<Flux<String>> dataCaptor;

    private NotificationClient notificationClient;

    @BeforeEach
    void setUp() {
        notificationClient = new NotificationClient(rSocketRequesterBuilder, objectMapper);

        // Set fields via reflection since they're @Value fields
        try {
            var hostField = NotificationClient.class.getDeclaredField("rsocketHost");
            hostField.setAccessible(true);
            hostField.set(notificationClient, "notification-service");

            var portField = NotificationClient.class.getDeclaredField("rsocketPort");
            portField.setAccessible(true);
            portField.set(notificationClient, 7000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldStreamAnswerSuccessfully() throws Exception {
        // Given
        String userId = "user-123";
        String requestId = "req-456";
        Flux<String> tokens = Flux.just("token1", "token2", "token3");
        String metadataJson = "{\"userId\":\"user-123\",\"requestId\":\"req-456\"}";

        RSocketRequester.RequestSpec requestSpec = mock(RSocketRequester.RequestSpec.class);
        RSocketRequester.RetrieveSpec retrieveSpec = mock(RSocketRequester.RetrieveSpec.class);

        when(rSocketRequesterBuilder.tcp("notification-service", 7000)).thenReturn(rSocketRequester);
        when(rSocketRequester.route("search.result")).thenReturn(requestSpec);
        when(requestSpec.metadata(any(String.class), any(MimeType.class))).thenReturn(requestSpec);
        when(requestSpec.data(any(Flux.class))).thenReturn(retrieveSpec);
        when(retrieveSpec.retrieveFlux(Void.class)).thenReturn(Flux.empty());

        when(objectMapper.writeValueAsString(Map.of("userId", userId, "requestId", requestId)))
                .thenReturn(metadataJson);

        // When
        notificationClient.streamAnswer(userId, requestId, tokens);

        // Then
        verify(rSocketRequesterBuilder).tcp("notification-service", 7000);
        verify(rSocketRequester).route("search.result");
        verify(requestSpec).metadata(metadataCaptor.capture(), mimeTypeCaptor.capture());
        verify(requestSpec).data(dataCaptor.capture());
        verify(retrieveSpec).retrieveFlux(Void.class);

        assertThat(metadataCaptor.getValue()).isEqualTo(metadataJson);
        assertThat(mimeTypeCaptor.getValue().toString()).isEqualTo("application/json");

        // Verify data flux
        Flux<String> capturedData = dataCaptor.getValue();
        assertThat(capturedData.collectList().block()).containsExactly("token1", "token2", "token3");
    }

    @Test
    void shouldHandleJsonSerializationError() throws Exception {
        // Given
        String userId = "user-123";
        String requestId = "req-456";
        Flux<String> tokens = Flux.just("token1");

        when(objectMapper.writeValueAsString(Map.of("userId", userId, "requestId", requestId)))
                .thenThrow(new RuntimeException("Serialization error"));

        // When/Then
        RuntimeException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> notificationClient.streamAnswer(userId, requestId, tokens)
        );

        assertThat(thrown).isNotNull();
        // Note: rSocketRequesterBuilder.tcp() is called before serialization error,
        // so it will be invoked even if serialization fails
    }

    @Test
    void shouldHandleRSocketError() throws Exception {
        // Given
        String userId = "user-123";
        String requestId = "req-456";
        Flux<String> tokens = Flux.just("token1");
        String metadataJson = "{\"userId\":\"user-123\",\"requestId\":\"req-456\"}";

        RSocketRequester.RequestSpec requestSpec = mock(RSocketRequester.RequestSpec.class);
        RSocketRequester.RetrieveSpec retrieveSpec = mock(RSocketRequester.RetrieveSpec.class);

        when(rSocketRequesterBuilder.tcp("notification-service", 7000)).thenReturn(rSocketRequester);
        when(rSocketRequester.route("search.result")).thenReturn(requestSpec);
        when(requestSpec.metadata(any(String.class), any(MimeType.class))).thenReturn(requestSpec);
        when(requestSpec.data(any(Flux.class))).thenReturn(retrieveSpec);
        when(retrieveSpec.retrieveFlux(Void.class)).thenReturn(Flux.error(new RuntimeException("RSocket error")));

        when(objectMapper.writeValueAsString(Map.of("userId", userId, "requestId", requestId)))
                .thenReturn(metadataJson);

        // When
        notificationClient.streamAnswer(userId, requestId, tokens);

        // Then - should not throw, just log error
        verify(rSocketRequesterBuilder).tcp("notification-service", 7000);
        verify(rSocketRequester).route("search.result");
    }
}