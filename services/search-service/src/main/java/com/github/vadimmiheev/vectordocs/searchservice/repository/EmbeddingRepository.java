package com.github.vadimmiheev.vectordocs.searchservice.repository;

import com.github.vadimmiheev.vectordocs.searchservice.entity.Embedding;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmbeddingRepository extends JpaRepository<Embedding, Long> {

    @Query(
    value = "SELECT * FROM embeddings e " +
            "WHERE (e.user_id = :userId) " +
            "ORDER BY e.vector <=> CAST(:queryVector AS vector)",
    nativeQuery = true)
    List<Embedding> findTopSimilar(@Param("userId") String userId,
                                   @Param("queryVector") String queryVector,
                                   Pageable pageable);
}
