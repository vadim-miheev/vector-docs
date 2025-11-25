package com.github.vadimmiheev.vectordocs.storageservice.controller;

import com.github.vadimmiheev.vectordocs.storageservice.dto.DocumentResponse;
import com.github.vadimmiheev.vectordocs.storageservice.entity.Document;
import com.github.vadimmiheev.vectordocs.storageservice.exception.ResourceNotFoundException;
import com.github.vadimmiheev.vectordocs.storageservice.service.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentStorageService storageService;
    private final Environment env;

    private boolean isDemoUser(String userId) {
        try {
            String prop = env.getProperty("DEMO_USER_ID", "0");
            int demoId = Integer.parseInt(prop == null ? "0" : prop);
            if (demoId <= 0) return false;
            return Integer.parseInt(userId) == demoId;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> list(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(storageService.getUserDocuments(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getById(@PathVariable("id") String id,
                                                    @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(storageService.getById(userId, id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable("id") String id,
                                             @RequestHeader("X-User-Id") String userId) {
        Document doc = storageService.getDocument(userId, id);
        Path path = Paths.get(doc.getPath());
        PathResource resource = new PathResource(path);
        if (!resource.exists()) {
            throw new ResourceNotFoundException("File not found on disk");
        }
        String filename = doc.getName();
        String contentType = doc.getContentType() != null ? doc.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(@RequestHeader("X-User-Id") String userId,
                                                   @RequestPart("file") MultipartFile file) {
        if (isDemoUser(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Demo user is not allowed to upload documents");
        }
        return ResponseEntity.ok(storageService.save(userId, file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String id,
                                       @RequestHeader("X-User-Id") String userId) {
        if (isDemoUser(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Demo user is not allowed to delete documents");
        }
        long result = storageService.delete(userId, id);
        if (result == 0) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.noContent().build();
    }
}
