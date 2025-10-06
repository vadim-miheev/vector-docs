package com.github.vadimmiheev.vectordocs.documentprocessor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentDeletedEvent {
    private UUID documentId;
    private String userId;
    private Instant deletedAt;

    @Override
    public String toString() {
        return "DocumentDeletedEvent{" +
                "documentId=" + documentId +
                ", userId='" + userId + '\'' +
                ", deletedAt=" + deletedAt +
                '}';
    }
}
