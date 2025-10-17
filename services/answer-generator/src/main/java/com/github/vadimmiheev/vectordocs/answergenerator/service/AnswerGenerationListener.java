package com.github.vadimmiheev.vectordocs.answergenerator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.answergenerator.dto.SearchProcessedEvent;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerGenerationListener {

    private final ObjectMapper objectMapper;
    private final OpenAiStreamingChatModel streamingChatModel;
    private final NotificationClient notificationClient;

    private static final String SYSTEM_PROMPT = String.join("\n",
            "You are part of a retrieval-augmented generation (RAG) system.",
            "The user is having an ongoing conversation with an assistant.\n",

            "Your role:",
            "- Answer the user's question using only the provided document fragments (embeddings).",
            "- You will receive:",
            "1. The conversation history.",
            "2. Document fragments array in JSON format, each with a file identifier (`fileUuid`), file name (`fileName`) and page number (`pageNumber`).\n",

            "Instructions:",
            "1. Use **only** the provided fragments to generate your answer. Do not rely on external knowledge.",
            "2. If the provided fragments do not contain enough information — explicitly state that the documents do not contain the answer.",
            "3. Each factual statement in your answer must reference one or more sources (document + page).",
            "4. If multiple fragments contain relevant information — **prefer to include all of them** as sources.",
            "5. Be concise, clear, and factual.",
            "6. Include **all relevant sources** that were used in forming the answer.\n",


            "Answer format:",
            "1. First, write the concise answer text.",
            "2. Then output the list of sources in format: <fileUuid>/<fileName>/<pageNumber>\\n",
            "3. Wrap the list in `<BEGIN_SOURCES>` and `<END_SOURCES>` tags exactly as shown.\n",

            "Example output:",
            "The warranty period specified in the documents is 2 years.",
            "<BEGIN_SOURCES>",
            "abc123/doc1.pdf/5",
            "xyz789/doc 2.txt/7",
            "<END_SOURCES>"
    );

    @KafkaListener(topics = "${app.topics.search-processed:search.processed}", groupId = "${spring.kafka.consumer.group-id:answer-generator}")
    public void onSearchProcessed(String message,
                                  @Header(name = "kafka_receivedMessageKey", required = false) String key) {
        try {
            SearchProcessedEvent event = objectMapper.readValue(message, SearchProcessedEvent.class);
            String userId = event.getUserId();
            String requestId = event.getRequestId();
            String query = event.getQuery();
            List<SearchProcessedEvent.SearchContextItem> context = event.getContext();
            List<SearchProcessedEvent.Hit> embeddings = event.getEmbeddings();

            if (!StringUtils.hasText(userId) || !StringUtils.hasText(query)) {
                log.warn("Skip processing: missing userId or query. key={}, userId={}, queryPresent={}", key, userId, StringUtils.hasText(query));
                return;
            }

            if (embeddings == null || embeddings.isEmpty()) {
                Flux<String> emptyFlux = Flux.just("Documents do not contain an answer to the question.\n");
                notificationClient.streamAnswer(userId, requestId, emptyFlux);
                log.info("No embeddings provided; streamed insufficiency message for userId={}", userId);
                return;
            }

            // Create a sink and stream tokens to notification-service
            Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
            notificationClient.streamAnswer(userId, requestId, sink.asFlux());

            streamingChatModel.generate(
                buildMessages(query, context, embeddings),
                new StreamingResponseHandler<>() {
                    @Override
                    public void onNext(String token) {
                        sink.tryEmitNext(token);
                    }

                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        sink.tryEmitComplete();
                        log.info("Completed streaming answer for userId={}", userId);
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("Error during LLM streaming for userId={}", userId, error);
                        sink.tryEmitError(error);
                    }
                }
            );

        } catch (Exception e) {
            log.error("Failed to process search.processed message: {}", message, e);
        }
    }

    private List<ChatMessage> buildMessages(String query, List<SearchProcessedEvent.SearchContextItem> context, List<SearchProcessedEvent.Hit> embeddings) {
        List<ChatMessage> messages = new ArrayList<>();

        messages.add(SystemMessage.from(SYSTEM_PROMPT));

        // provide chat history
        if (context != null && !context.isEmpty()) {
            for (SearchProcessedEvent.SearchContextItem item : context) {
                if (item.getRole().equals("agent")) {
                    messages.add(AiMessage.from(item.getMessage()));
                } else if (item.getRole().equals("user")) {
                    messages.add(UserMessage.from(item.getMessage()));
                }
            }
        }

        // last user message
        messages.add(UserMessage.from(query));

        // fragments
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Fragments:\n");
            String fragments = objectMapper.writeValueAsString(embeddings);
            sb.append(fragments);
            messages.add(SystemMessage.from(sb.toString()));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize fragments", e);
        }

        return messages;
    }
}
