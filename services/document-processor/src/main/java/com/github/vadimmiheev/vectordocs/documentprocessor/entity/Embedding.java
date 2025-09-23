package com.github.vadimmiheev.vectordocs.documentprocessor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "embeddings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Embedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_uuid", nullable = false)
    private UUID fileUuid;

    @Column(name = "chunk_text", columnDefinition = "TEXT", nullable = false)
    private String chunkText;

    // Store vector as a TEXT (comma-separated floats) for portability; can be migrated to vector/array later
    @Column(name = "vector", columnDefinition = "TEXT", nullable = false)
    private String vector;

    @Column(name = "page_number")
    private Integer pageNumber; // optional

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "user_id", nullable = false)
    private String userId;
}
