package com.github.vadimmiheev.vectordocs.documentprocessor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentUploadedEvent {
    private UUID id;
    private String name;
    private long size;
    private String userId;
    private String contentType;
    private Instant createdAt;
    private URI downloadUrl;

    @Override
    public String toString() {
        return "DocumentUploadedEvent{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", size=" + size +
                ", userId='" + userId + '\'' +
                ", contentType='" + contentType + '\'' +
                ", createdAt=" + createdAt +
                ", downloadUrl=" + downloadUrl +
                '}';
    }
}
