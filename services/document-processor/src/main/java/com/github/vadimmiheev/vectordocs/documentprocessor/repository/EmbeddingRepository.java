package com.github.vadimmiheev.vectordocs.documentprocessor.repository;

import com.github.vadimmiheev.vectordocs.documentprocessor.entity.Embedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmbeddingRepository extends JpaRepository<Embedding, Long> {
    List<Embedding> findByFileUuid(UUID fileUuid);
}
