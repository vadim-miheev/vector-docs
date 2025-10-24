package com.github.vadimmiheev.vectordocs.documentprocessor.repository;

import com.github.vadimmiheev.vectordocs.documentprocessor.entity.Embedding;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmbeddingRepository extends JpaRepository<Embedding, Long> {
    void deleteByFileUuid(UUID fileUuid);

    long countByFileUuidAndVectorGeneratedFalse(UUID fileUuid);

    long countByFileUuid(UUID fileUuid);

    List<Embedding> findByFileUuidAndVectorGenerated(UUID fileUuid, boolean vectorGenerated, Limit limit);

    List<Embedding> findByVectorGeneratedFalse();
}
