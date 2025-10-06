package com.github.vadimmiheev.vectordocs.storageservice.event;

import com.github.vadimmiheev.vectordocs.storageservice.dto.DocumentResponse;

/**
 * Domain event indicating that a document was uploaded.
 */
public record DocumentUploadedEvent(DocumentResponse document) {}