package com.github.vadimmiheev.vectordocs.searchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchProcessedEvent {
    private String query;
    private String userId;
    private Map<String, String> context;
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
