package com.github.vadimmiheev.vectordocs.storageservice.repository;

import com.github.vadimmiheev.vectordocs.storageservice.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, String> {
    List<Document> findAllByUserIdOrderByCreatedAtDesc(String userId);
    Optional<Document> findByIdAndUserId(String id, String userId);
    long deleteByIdAndUserId(String id, String userId);
}
