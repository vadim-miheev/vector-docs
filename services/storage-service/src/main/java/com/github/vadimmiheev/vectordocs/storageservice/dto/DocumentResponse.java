package com.github.vadimmiheev.vectordocs.storageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@AllArgsConstructor
public class DocumentResponse {
    private String id;
    private String name;
    private long size;
    private String userId;
    private String contentType;
    private Instant createdAt;
    private String downloadUrl;
    private boolean processed;
}
