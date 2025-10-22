package com.github.vadimmiheev.vectordocs.documentprocessor.event;

import java.util.UUID;

public record EmbeddingsGeneratedEvent(UUID fileUuid, String userId, String fileName) {}
