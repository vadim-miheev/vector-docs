package com.github.vadimmiheev.vectordocs.documentprocessor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    // vector column with fixed dimension 768
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 768)
    @Column(name = "vector", nullable = false)
    private float[] vector;

    @Column(name = "page_number")
    private Integer pageNumber; // optional

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "user_id", nullable = false)
    private String userId;
}
