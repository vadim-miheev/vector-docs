package com.github.vadimmiheev.vectordocs.storageservice.event;

/**
 * Domain event indicating that a document was deleted.
 */
public record DocumentDeletedEvent(String documentId, String userId) {}