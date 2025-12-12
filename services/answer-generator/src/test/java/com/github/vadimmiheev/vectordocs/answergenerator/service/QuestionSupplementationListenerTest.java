package com.github.vadimmiheev.vectordocs.answergenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.answergenerator.dto.SearchRequestEvent;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionSupplementationListenerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OpenAiChatModel chatModel;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private QuestionSupplementationListener listener;

    @BeforeEach
    void setUp() {
        listener = new QuestionSupplementationListener(objectMapper, chatModel, kafkaTemplate);
        // Set topic via reflection since it's @Value field
        try {
            var field = QuestionSupplementationListener.class.getDeclaredField("supplementedRequestTopic");
            field.setAccessible(true);
            field.set(listener, "search.request.supplemented");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldSkipProcessingWhenMissingUserId() throws Exception {
        // Given
        String message = "{\"query\":\"test query\"}";
        SearchRequestEvent request = new SearchRequestEvent();
        request.setQuery("test query");
        // userId is null

        when(objectMapper.readValue(message, SearchRequestEvent.class)).thenReturn(request);

        // When
        listener.onSearchRequest(message, "key-123");

        // Then
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void shouldSkipProcessingWhenMissingQuery() throws Exception {
        // Given
        String message = "{\"userId\":\"user-123\"}";
        SearchRequestEvent request = new SearchRequestEvent();
        request.setUserId("user-123");
        // query is null

        when(objectMapper.readValue(message, SearchRequestEvent.class)).thenReturn(request);

        // When
        listener.onSearchRequest(message, "key-123");

        // Then
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void shouldProcessRequestWithoutContext() throws Exception {
        // Given
        String message = "{\"userId\":\"user-123\",\"query\":\"test query\"}";
        SearchRequestEvent request = new SearchRequestEvent();
        request.setUserId("user-123");
        request.setQuery("test query");
        request.setContext(new ArrayList<>()); // empty context

        when(objectMapper.readValue(message, SearchRequestEvent.class)).thenReturn(request);
        when(objectMapper.writeValueAsString(any())).thenReturn("serialized-message");

        // When
        listener.onSearchRequest(message, "key-123");

        // Then
        verify(kafkaTemplate).send(eq("search.request.supplemented"), eq("key-123"), eq("serialized-message"));
        assertThat(request.getRagQuery()).isNull(); // Should not be set when no context
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldProcessRequestWithContextAndGenerateRagQuery() throws Exception {
        // Given
        String message = "{\"userId\":\"user-123\",\"query\":\"current question\",\"context\":[{\"role\":\"user\",\"message\":\"previous question\"}]}";
        SearchRequestEvent request = new SearchRequestEvent();
        request.setUserId("user-123");
        request.setQuery("current question");
        request.setContext(new ArrayList<>(List.of(
                new SearchRequestEvent.SearchContextItem("user", "previous question")
        )));

        dev.langchain4j.model.output.Response<AiMessage> response =
                dev.langchain4j.model.output.Response.from(AiMessage.from("enhanced search query"));

        when(objectMapper.readValue(message, SearchRequestEvent.class)).thenReturn(request);
        when(chatModel.generate(any(List.class))).thenReturn(response);
        when(objectMapper.writeValueAsString(any())).thenReturn("serialized-message");

        // When
        listener.onSearchRequest(message, "key-123");

        // Then
        verify(kafkaTemplate).send(eq("search.request.supplemented"), eq("key-123"), eq("serialized-message"));
        assertThat(request.getRagQuery()).isEqualTo("enhanced search query");
    }

    @Test
    void shouldHandleExceptionDuringProcessing() throws Exception {
        // Given
        String message = "invalid-json";
        when(objectMapper.readValue(message, SearchRequestEvent.class)).thenThrow(new RuntimeException("Parse error"));

        // When
        listener.onSearchRequest(message, "key-123");

        // Then
        // Should not throw exception, just log error
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}