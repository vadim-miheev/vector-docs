package com.github.vadimmiheev.vectordocs.answergenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.answergenerator.dto.SearchRequestEvent;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionSupplementationListener {

    private final ObjectMapper objectMapper;
    private final OpenAiChatModel chatModel;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.topics.search-request-supplemented}")
    private String supplementedRequestTopic;

    private static final String SYSTEM_PROMPT = String.join("\n",
            "You are part of a retrieval-augmented generation (RAG) system.",
            "The user is having an ongoing conversation with an assistant.",
            "Your task is to create a single, clear, and information-rich search query",
            "that captures the user’s current information need based on the entire conversation so far.",
            "Use the full context of the conversation (both user and assistant messages) to understand what the user is asking about right now.",
            "Focus on the key topic and the user’s intent, not on casual or irrelevant parts of the chat.",
            "Return ONLY the search query as plain text, without explanations, formatting, or quotes.",
            "Generate the final search query that best represents what information should be retrieved from the database."
    );

    @KafkaListener(topics = "${app.topics.search-request:search.request}", groupId = "${spring.kafka.consumer.group-id:answer-generator}")
    public void onSearchRequest(String message,
                                @Header(name = "kafka_receivedMessageKey", required = false) String key) {
        try {
            SearchRequestEvent request = objectMapper.readValue(message, SearchRequestEvent.class);
            String userId = request.getUserId();
            String query = request.getQuery();

            if (!StringUtils.hasText(userId) || !StringUtils.hasText(query)) {
                log.warn("Skip processing: missing userId or query. key={}, userId={}, queryPresent={}", key, userId, StringUtils.hasText(query));
                return;
            }

            // Skip supplementation if there is no context
            if (!request.getContext().isEmpty()) {
                AiMessage searchQuery = chatModel.generate(buildMessages(query, request.getContext())).content();
                request.setRagQuery(searchQuery.text());
            }

            kafkaTemplate.send(supplementedRequestTopic, key, objectMapper.writeValueAsString(request));
            log.info("Published search.request.supplemented for userId={}, query={} to topic {}", userId, query, supplementedRequestTopic);
        } catch (Exception e) {
            log.error("Failed to process search.request message: {}", message, e);
        }
    }

    private List<ChatMessage> buildMessages(String query, List<SearchRequestEvent.SearchContextItem> context) {
        List<ChatMessage> messages = new ArrayList<>();

        messages.add(SystemMessage.from(SYSTEM_PROMPT));

        // provide chat history
        if (context != null && !context.isEmpty()) {
            for (SearchRequestEvent.SearchContextItem item : context) {
                if (item.getRole().equals("agent")) {
                    messages.add(AiMessage.from(item.getMessage()));
                } else if (item.getRole().equals("user")) {
                    messages.add(UserMessage.from(item.getMessage()));
                }
            }
        }

        // last user message
        messages.add(UserMessage.from(query));

        return messages;
    }
}
