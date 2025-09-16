package com.github.vadimmiheev.vectordocs.storageservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vadimmiheev.vectordocs.storageservice.dto.DocumentResponse;
import com.github.vadimmiheev.vectordocs.storageservice.entity.Document;
import com.github.vadimmiheev.vectordocs.storageservice.exception.InvalidFileTypeException;
import com.github.vadimmiheev.vectordocs.storageservice.exception.ResourceNotFoundException;
import com.github.vadimmiheev.vectordocs.storageservice.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
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
public class DocumentStorageService {

    private final DocumentRepository documentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.topics.documents-uploaded}")
    private String documentsUploadedTopic;

    public DocumentStorageService(DocumentRepository documentRepository,
                                  KafkaTemplate<String, String> kafkaTemplate,
                                  ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

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

        Path userDir = Paths.get(System.getenv("ROOT_STORAGE_PATH"), userId);
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

            documentRepository.save(doc);

            DocumentResponse response = toResponse(doc);
            response.setDownloadUrl(generateDownloadUrl(doc, false)); // replace download url with an internal link
            // Publish event to Kafka (non-blocking, best-effort)
            try {
                String payload = objectMapper.writeValueAsString(response);
                kafkaTemplate.send(documentsUploadedTopic, doc.getId(), payload);
                log.info("Published event to topic '{}' for document id={} userId={}", documentsUploadedTopic, doc.getId(), userId);
            } catch (Exception ex) {
                log.error("Failed to publish '{}' event for document id={} userId={}", documentsUploadedTopic, doc.getId(), userId, ex);
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
        return documentRepository.deleteByIdAndUserId(id, userId);
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
                generateDownloadUrl(d, true)
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
        String host = publicLink ? System.getenv("STORAGE_SERVICE_HOST") : System.getenv("INTERNAL_HOST");
        return host + "/documents/" + document.getId() + "/download";
    }
}
