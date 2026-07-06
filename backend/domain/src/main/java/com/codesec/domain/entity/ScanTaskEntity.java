package com.codesec.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scan_task", indexes = {
    @Index(name = "idx_repo_status", columnList = "repo_id, status"),
    @Index(name = "idx_started_at", columnList = "started_at"),
    @Index(name = "idx_repo_commit_created", columnList = "repo_id, commit_sha, created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanTaskEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_id", nullable = false)
    private Long repoId;

    @Column(nullable = false, length = 256)
    private String branch = "main";

    @Column(name = "commit_sha", length = 64)
    private String commitSha;

    @Column(nullable = false, length = 20)
    private String status = "queued";

    @Column(nullable = false, length = 32)
    private String engine = "self_sast";

    @Column(nullable = false, length = 16)
    private String mode = "full";

    @Column(name = "scan_request_id", length = 64)
    private String scanRequestId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
