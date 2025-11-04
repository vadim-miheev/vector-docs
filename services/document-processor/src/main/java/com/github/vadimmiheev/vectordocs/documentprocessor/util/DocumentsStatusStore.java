package com.github.vadimmiheev.vectordocs.documentprocessor.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DocumentsStatusStore {

    private static final ConcurrentHashMap<String, DocumentStatus> STATUSES = new ConcurrentHashMap<>();

    public static boolean isCancelled(String documentId) {
        DocumentStatus status = STATUSES.get(documentId);
        return status != null && status.isCancelled();
    }

    public static void cancel(String documentId) {
        DocumentStatus status = STATUSES.get(documentId);
        Objects.requireNonNullElseGet(status, () -> register(documentId)).cancel();
    }

    private static DocumentStatus register(String documentId) {
        DocumentStatus status = new DocumentStatus(documentId);
        STATUSES.put(documentId, status);
        return status;
    }

    @RequiredArgsConstructor
    private static class DocumentStatus {
        private final String documentId;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        public void cancel() { cancelled.set(true); }
        public boolean isCancelled() { return cancelled.get(); }
    }
}
