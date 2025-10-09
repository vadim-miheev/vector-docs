package com.github.vadimmiheev.vectordocs.searchservice.dto;

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
    private List<SearchRequestEvent.SearchContextItem> context;
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
}
