package com.github.vadimmiheev.vectordocs.searchservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.searchservice.dto.SearchProcessedEvent;
import com.github.vadimmiheev.vectordocs.searchservice.dto.SearchRequestEvent;
import com.github.vadimmiheev.vectordocs.searchservice.entity.Embedding;
import com.github.vadimmiheev.vectordocs.searchservice.repository.EmbeddingRepository;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchRequestListener {

    private final EmbeddingRepository embeddingRepository;
    private final EmbeddingModel embeddingModel;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.topics.search-processed:search.processed}")
    private String processedTopic;

    @Value("${app.search.top-k:5}")
    private int topK;

    @KafkaListener(topics = "${app.topics.search-request}", groupId = "${spring.kafka.consumer.group-id:search-service}")
    public void onSearchRequest(String message,
                                @Header(name = "kafka_receivedMessageKey", required = false) String key) {
        try {
            SearchRequestEvent request = objectMapper.readValue(message, SearchRequestEvent.class);
            String userId = request.getUserId();
            String query = request.getQuery();
            ArrayList<SearchRequestEvent.SearchContextItem> context = request.getContext();

            if (!StringUtils.hasText(userId) || !StringUtils.hasText(query)) {
                log.warn("Skip processing: missing userId or query. key={}, userId={}, queryPresent={}", key, userId, StringUtils.hasText(query));
                return;
            }

            // 1) Build query embedding
            Response<dev.langchain4j.data.embedding.Embedding> response = embeddingModel.embed(query);
            float[] queryVector = response.content().vector();
			String pgVectorString = Arrays.toString(queryVector);

            // 2) Fetch top-K similar chunks for this user
            List<Embedding> hits = embeddingRepository.findTopSimilar(userId, pgVectorString, PageRequest.of(0, topK));

            // 3) Map to processed event
            List<SearchProcessedEvent.Hit> embeddings = hits.stream()
                    .map(e -> SearchProcessedEvent.Hit.builder()
                            .fileUuid(e.getFileUuid())
                            .pageNumber(e.getPageNumber())
                            .chunkText(e.getChunkText())
                            .build())
                    .collect(Collectors.toList());

            SearchProcessedEvent processed = SearchProcessedEvent.builder()
                    .query(query)
                    .userId(userId)
                    .context(context)
                    .embeddings(embeddings)
                    .build();

            String payload = objectMapper.writeValueAsString(processed);
            kafkaTemplate.send(processedTopic, userId, payload);
            log.info("Published search.processed for userId={}, embeddings={} to topic {}", userId, embeddings.size(), processedTopic);
        } catch (Exception e) {
            log.error("Failed to process search.request message: {}", message, e);
        }
    }
}
