package com.github.vadimmiheev.vectordocs.answergenerator.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatModelConfig {

    @Bean
    public OpenAiChatModel chatModel(
            @Value("${app.chat.base-url:http://localhost:11434/v1}") String baseUrl,
            @Value("${app.chat.model-name:llama3.1}") String modelName,
            @Value("${app.chat.api-key:dummy}") String apiKey
    ) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .apiKey(apiKey)
                .build();
    }

    @Bean
    public OpenAiStreamingChatModel streamingChatModel(
            @Value("${app.chat.base-url:http://localhost:11434/v1}") String baseUrl,
            @Value("${app.chat.model-name:llama3.1}") String modelName,
            @Value("${app.chat.api-key:dummy}") String apiKey
    ) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .apiKey(apiKey)
                .build();
    }
}
