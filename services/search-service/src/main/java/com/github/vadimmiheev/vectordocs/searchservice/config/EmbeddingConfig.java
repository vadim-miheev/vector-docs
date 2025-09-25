package com.github.vadimmiheev.vectordocs.searchservice.config;

import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Bean
    public OpenAiEmbeddingModel embeddingModel(
            @Value("${app.embedding.base-url:http://localhost:11434/v1}") String baseUrl,
            @Value("${app.embedding.model-name:nomic-embed-text}") String modelName,
            @Value("${app.embedding.api-key:dummy}") String apiKey
    ) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .apiKey(apiKey)
                .build();
    }
}
