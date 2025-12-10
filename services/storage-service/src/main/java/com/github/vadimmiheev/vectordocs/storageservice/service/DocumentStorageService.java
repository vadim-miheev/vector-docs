package com.github.vadimmiheev.vectordocs.storageservice.service;

import com.github.vadimmiheev.vectordocs.storageservice.dto.DocumentResponse;
import com.github.vadimmiheev.vectordocs.storageservice.entity.Document;
import com.github.vadimmiheev.vectordocs.storageservice.event.DocumentDeletedEvent;
import com.github.vadimmiheev.vectordocs.storageservice.event.DocumentUploadedEvent;
import com.github.vadimmiheev.vectordocs.storageservice.exception.InvalidFileTypeException;
import com.github.vadimmiheev.vectordocs.storageservice.exception.ResourceNotFoundException;
import com.github.vadimmiheev.vectordocs.storageservice.repository.DocumentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentStorageService {

    private final DocumentRepository documentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${ROOT_STORAGE_PATH:/tmp/storage}")
    @Setter(AccessLevel.PACKAGE)
    private String rootStoragePath;

    @Value("${STORAGE_SERVICE_HOST:http://localhost:8080}")
    @Setter(AccessLevel.PACKAGE)
    private String storageServiceHost;

    @Value("${INTERNAL_HOST:http://storage-service}")
    @Setter(AccessLevel.PACKAGE)
    private String internalHost;

    public List<DocumentResponse> getUserDocuments(String userId) {
        return documentRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DocumentResponse save(String userId, MultipartFile file) {
        validate(userId, file);
        String id = UUID.randomUUID().toString();
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        originalName = sanitizeFilename(originalName);
        String contentType = file.getContentType() != null ? file.getContentType() : guessContentType(originalName);
        if (!isAllowed(contentType, originalName)) {
            throw new InvalidFileTypeException("Only PDF and TXT files are allowed");
        }

        Path userDir = Paths.get(rootStoragePath, userId);
        try {
            Files.createDirectories(userDir);
            Path target = userDir.resolve(id);
            file.transferTo(target);

            Document doc = new Document();
            doc.setId(id);
            doc.setName(originalName);
            doc.setSize(file.getSize());
            doc.setUserId(userId);
            doc.setContentType(contentType);
            doc.setPath(userDir.resolve(id).toString());
            doc.setCreatedAt(Instant.now());
            doc.setStatus("uploaded");

            documentRepository.save(doc);

            DocumentResponse response = toResponse(doc);
            response.setDownloadUrl(generateDownloadUrl(doc, false)); // replace download url with an internal link
            // Publish domain event to be handled after transaction commit
            try {
                eventPublisher.publishEvent(new DocumentUploadedEvent(response));
            } catch (Exception ex) {
                log.error("Failed to prepare '{}' event for document id={} userId={}", "documents-uploaded", doc.getId(), userId, ex);
            }

            return toResponse(doc);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Transactional
    public long delete(String userId, String id) {
        Document doc = documentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        // Delete a file if exists
        try {
            Path p = Paths.get(doc.getPath());
            Files.deleteIfExists(p);
        } catch (IOException e) {
            log.error("Failed to delete file {}", doc.getPath(), e);
            return 0;
        }
        long deleted = documentRepository.deleteByIdAndUserId(doc.getId(), doc.getUserId());
        if (deleted > 0) {
            // Publish domain event to be handled after transaction commit
            eventPublisher.publishEvent(new DocumentDeletedEvent(doc.getId(), doc.getUserId()));
        }
        return deleted;
    }

    public DocumentResponse getById(String userId, String id) {
        return toResponse(getDocument(userId, id));
    }

    public Document getDocument(String userId, String id) {
        return documentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    private void validate(String userId, MultipartFile file) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
    }

    private boolean isAllowed(String contentType, String filename) {
        String lower = filename.toLowerCase();
        boolean byExt = lower.endsWith(".pdf") || lower.endsWith(".txt");
        boolean byType = "application/pdf".equalsIgnoreCase(contentType) || "text/plain".equalsIgnoreCase(contentType);
        return byExt || byType;
    }

    private String guessContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }

    private DocumentResponse toResponse(Document d) {
        return new DocumentResponse(
                d.getId(),
                d.getName(),
                d.getSize(),
                d.getUserId(),
                d.getContentType(),
                d.getCreatedAt(),
                generateDownloadUrl(d, true),
                d.getStatus()
        );
    }

    private String sanitizeFilename(String name) {
        String sanitized = name.replaceAll("[\\\\/]+", "_");
        // prevent traversal and hidden names
        sanitized = sanitized.replace("..", "_");
        if (sanitized.isBlank()) {
            return "document";
        }
        return sanitized;
    }

    public String generateDownloadUrl(Document document, boolean publicLink) {
        if (document == null || document.getId() == null) {
            return null;
        }
        // Current service private host
        String host = publicLink ? storageServiceHost : internalHost;
        return host + "/documents/" + document.getId() + "/download";
    }
}
