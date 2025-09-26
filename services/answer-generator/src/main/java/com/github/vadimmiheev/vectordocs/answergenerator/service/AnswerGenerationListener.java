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
            "You are an assistant that answers user questions based on the provided documents.",
            "You are given the user’s query and a list of text fragments from documents with file identifiers and page numbers.",
            "Your task:",
            "Use only the provided fragments (embeddings) to answer.",
            "If the information is insufficient — explicitly state that the documents do not contain the answer.",
            "In the answer, always include the document identifier (fileUuid) and page number (pageNumber) for each statement.",
            "Respond briefly, clearly, and to the point.",
            "Answer format:",
            "First, provide a concise and accurate answer to the user’s question.",
            "Then, list the sources in the format:",
            "Document: <fileUuid>, page <pageNumber>"
    );

    @KafkaListener(topics = "${app.topics.search-processed:search.processed}", groupId = "${spring.kafka.consumer.group-id:answer-generator}")
    public void onSearchProcessed(String message,
                                  @Header(name = "kafka_receivedMessageKey", required = false) String key) {
        try {
            SearchProcessedEvent event = objectMapper.readValue(message, SearchProcessedEvent.class);
            String userId = event.getUserId();
            String query = event.getQuery();
            Map<String, String> context = event.getContext();
            List<SearchProcessedEvent.Hit> embeddings = event.getEmbeddings();

            if (!StringUtils.hasText(userId) || !StringUtils.hasText(query)) {
                log.warn("Skip processing: missing userId or query. key={}, userId={}, queryPresent={}", key, userId, StringUtils.hasText(query));
                return;
            }

            if (embeddings == null || embeddings.isEmpty()) {
                Flux<String> emptyFlux = Flux.just("Documents do not contain an answer to the question.\n");
                notificationClient.streamAnswer(userId, emptyFlux);
                log.info("No embeddings provided; streamed insufficiency message for userId={}", userId);
                return;
            }

            // Create a sink and stream tokens to notification-service
            Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
            notificationClient.streamAnswer(userId, sink.asFlux());

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

    private List<ChatMessage> buildMessages(String query, Map<String, String> context, List<SearchProcessedEvent.Hit> embeddings) {
        List<ChatMessage> messages = new ArrayList<>();

        messages.add(SystemMessage.from(SYSTEM_PROMPT));

        // provide chat history
        if (context != null && !context.isEmpty()) {
            for (Map.Entry<String, String> e : context.entrySet()) {
                if (e.getKey().equals("agent")) {
                    messages.add(AiMessage.from(e.getValue()));
                } else if (e.getKey().equals("user")) {
                    messages.add(UserMessage.from(e.getValue()));
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
