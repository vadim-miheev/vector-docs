package com.github.vadimmiheev.vectordocs.storageservice.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "documents",
    indexes = {
        @Index(name = "idx_doc_user_id_created_at", columnList = "user_id, created_at"),
        @Index(name = "idx_doc_id_user_id", columnList = "id, user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Document {
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id; // store as String to accept UUID or other ID formats

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "size", nullable = false)
    private long size;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "path", nullable = false)
    private String path; // filesystem path where file is stored

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "status")
    private String status = "new";

}
