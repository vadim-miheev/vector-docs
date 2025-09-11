package com.github.vadimmiheev.vectordocs.storageservice.controller;

import com.github.vadimmiheev.vectordocs.storageservice.dto.DocumentResponse;
import com.github.vadimmiheev.vectordocs.storageservice.service.DocumentStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentStorageService storageService;

    public DocumentController(DocumentStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> list(@RequestParam("userId") String userId) {
        return ResponseEntity.ok(storageService.getUserDocuments(userId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(@RequestParam("userId") String userId,
                                                   @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(storageService.save(userId, file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String id,
                                       @RequestParam("userId") String userId) {
        long result = storageService.delete(userId, id);
        if (result == 0) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.noContent().build();
    }
}
