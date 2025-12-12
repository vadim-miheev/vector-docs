package com.github.vadimmiheev.vectordocs.answergenerator.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChatModelConfigTest {

    @Test
    void shouldCreateChatModelWithDefaultValues() {
        // Given
        ChatModelConfig config = new ChatModelConfig();

        // When
        OpenAiChatModel chatModel = config.chatModel("http://localhost:11434/v1", "llama3.1", "dummy");

        // Then
        assertThat(chatModel).isNotNull();
        // Models should be created without exceptions
    }

    @Test
    void shouldCreateChatModelWithCustomValues() {
        // Given
        ChatModelConfig config = new ChatModelConfig();
        String customBaseUrl = "http://custom-llm:8080/v1";
        String customModelName = "custom-model";
        String customApiKey = "custom-key";

        // When
        OpenAiChatModel chatModel = config.chatModel(customBaseUrl, customModelName, customApiKey);

        // Then
        assertThat(chatModel).isNotNull();
        // Models should be created without exceptions
    }

    @Test
    void shouldCreateStreamingChatModelWithDefaultValues() {
        // Given
        ChatModelConfig config = new ChatModelConfig();

        // When
        OpenAiStreamingChatModel streamingChatModel = config.streamingChatModel("http://localhost:11434/v1", "llama3.1", "dummy");

        // Then
        assertThat(streamingChatModel).isNotNull();
        // Models should be created without exceptions
    }

    @Test
    void shouldCreateStreamingChatModelWithCustomValues() {
        // Given
        ChatModelConfig config = new ChatModelConfig();
        String customBaseUrl = "http://custom-llm:8080/v1";
        String customModelName = "custom-model";
        String customApiKey = "custom-key";

        // When
        OpenAiStreamingChatModel streamingChatModel = config.streamingChatModel(customBaseUrl, customModelName, customApiKey);

        // Then
        assertThat(streamingChatModel).isNotNull();
        // Models should be created without exceptions
    }
}