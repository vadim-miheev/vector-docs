package com.github.vadimmiheev.vectordocs.answergenerator.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchProcessedEvent {
    private String query;
    private String userId;
    private List<SearchContextItem> context;
    private List<Hit> embeddings;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Hit {
        private UUID fileUuid;
        private Integer pageNumber;
        private String chunkText;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class SearchContextItem {
        private String role;
        private String  message;
    }
}
