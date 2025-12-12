package com.github.vadimmiheev.vectordocs.answergenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.answergenerator.dto.SearchProcessedEvent;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnswerGenerationListenerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OpenAiStreamingChatModel streamingChatModel;

    @Mock
    private NotificationClient notificationClient;

    @Captor
    private ArgumentCaptor<StreamingResponseHandler<AiMessage>> handlerCaptor;

    private AnswerGenerationListener listener;

    @BeforeEach
    void setUp() {
        listener = new AnswerGenerationListener(objectMapper, streamingChatModel, notificationClient);
    }

    @Test
    void shouldSkipProcessingWhenMissingUserId() throws Exception {
        // Given
        String message = "{\"query\":\"test query\",\"requestId\":\"req-123\"}";
        SearchProcessedEvent event = new SearchProcessedEvent();
        event.setQuery("test query");
        event.setRequestId("req-123");
        // userId is null

        when(objectMapper.readValue(message, SearchProcessedEvent.class)).thenReturn(event);

        // When
        listener.onSearchProcessed(message, "key-123");

        // Then
        verify(notificationClient, never()).streamAnswer(any(), any(), any());
    }

    @Test
    void shouldSkipProcessingWhenMissingQuery() throws Exception {
        // Given
        String message = "{\"userId\":\"user-123\",\"requestId\":\"req-123\"}";
        SearchProcessedEvent event = new SearchProcessedEvent();
        event.setUserId("user-123");
        event.setRequestId("req-123");
        // query is null

        when(objectMapper.readValue(message, SearchProcessedEvent.class)).thenReturn(event);

        // When
        listener.onSearchProcessed(message, "key-123");

        // Then
        verify(notificationClient, never()).streamAnswer(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleEmptyEmbeddings() throws Exception {
        // Given
        String message = "{\"userId\":\"user-123\",\"query\":\"test query\",\"requestId\":\"req-123\"}";
        SearchProcessedEvent event = new SearchProcessedEvent();
        event.setUserId("user-123");
        event.setQuery("test query");
        event.setRequestId("req-123");
        // embeddings is null

        when(objectMapper.readValue(message, SearchProcessedEvent.class)).thenReturn(event);

        // When
        listener.onSearchProcessed(message, "key-123");

        // Then
        verify(notificationClient).streamAnswer(eq("user-123"), eq("req-123"), any(Flux.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldProcessSearchProcessedEventWithEmbeddings() throws Exception {
        // Given
        String message = "{\"userId\":\"user-123\",\"query\":\"test query\",\"requestId\":\"req-123\",\"embeddings\":[{\"fileUuid\":\"file-1\",\"fileName\":\"doc.pdf\",\"pageNumber\":1}]}";
        SearchProcessedEvent event = new SearchProcessedEvent();
        event.setUserId("user-123");
        event.setQuery("test query");
        event.setRequestId("req-123");
        event.setEmbeddings(List.of(
                SearchProcessedEvent.Hit.builder()
                        .fileUuid(java.util.UUID.randomUUID())
                        .fileName("doc.pdf")
                        .pageNumber(1)
                        .chunkText("content")
                        .build()
        ));

        when(objectMapper.readValue(message, SearchProcessedEvent.class)).thenReturn(event);

        // When
        listener.onSearchProcessed(message, "key-123");

        // Then
        verify(streamingChatModel).generate(any(List.class), handlerCaptor.capture());
        verify(notificationClient).streamAnswer(eq("user-123"), eq("req-123"), any(Flux.class));

        // Simulate streaming response
        StreamingResponseHandler<AiMessage> handler = handlerCaptor.getValue();
        handler.onNext("token1");
        handler.onNext("token2");
        handler.onComplete(null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleStreamingError() throws Exception {
        // Given
        String message = "{\"userId\":\"user-123\",\"query\":\"test query\",\"requestId\":\"req-123\",\"embeddings\":[{\"fileUuid\":\"file-1\",\"fileName\":\"doc.pdf\",\"pageNumber\":1}]}";
        SearchProcessedEvent event = new SearchProcessedEvent();
        event.setUserId("user-123");
        event.setQuery("test query");
        event.setRequestId("req-123");
        event.setEmbeddings(List.of(
                SearchProcessedEvent.Hit.builder()
                        .fileUuid(java.util.UUID.randomUUID())
                        .fileName("doc.pdf")
                        .pageNumber(1)
                        .chunkText("content")
                        .build()
        ));

        when(objectMapper.readValue(message, SearchProcessedEvent.class)).thenReturn(event);

        // When
        listener.onSearchProcessed(message, "key-123");

        // Then
        verify(streamingChatModel).generate(any(List.class), handlerCaptor.capture());

        // Simulate error
        StreamingResponseHandler<AiMessage> handler = handlerCaptor.getValue();
        handler.onError(new RuntimeException("Streaming error"));
    }

    @Test
    void shouldHandleExceptionDuringProcessing() throws Exception {
        // Given
        String message = "invalid-json";
        when(objectMapper.readValue(message, SearchProcessedEvent.class)).thenThrow(new RuntimeException("Parse error"));

        // When
        listener.onSearchProcessed(message, "key-123");

        // Then
        // Should not throw exception, just log error
        verify(notificationClient, never()).streamAnswer(any(), any(), any());
    }
}