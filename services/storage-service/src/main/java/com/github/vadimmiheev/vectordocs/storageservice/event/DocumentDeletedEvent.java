package com.github.vadimmiheev.vectordocs.storageservice.event;

import java.time.Instant;

/**
 * Domain event indicating that a document was deleted.
 */
public record DocumentDeletedEvent(String documentId, String userId, Instant deletedAt) {}