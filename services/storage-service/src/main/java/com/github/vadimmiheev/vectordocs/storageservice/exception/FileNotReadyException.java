package com.github.vadimmiheev.vectordocs.storageservice.exception;

public class FileNotReadyException extends RuntimeException {
    public FileNotReadyException(String message) {
        super(message);
    }
}
