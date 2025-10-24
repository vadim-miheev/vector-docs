package com.github.vadimmiheev.vectordocs.documentprocessor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProcessingEvent {
    private UUID id;
    private String userId;
    private String name;
    private int progressPercentage;
}