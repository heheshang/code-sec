package com.codesec.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "repo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepoEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 16)
    private String platform = "gitlab";

    @Column(name = "gitlab_project_id")
    private Long gitlabProjectId;

    @Column(nullable = false, length = 512)
    private String url;

    @Column(name = "access_token_encrypted", nullable = false, length = 512)
    private String accessTokenEncrypted;

    @Column(name = "webhook_secret", nullable = false, length = 128)
    private String webhookSecret = "";

    @Column(name = "default_branch", nullable = false, length = 128)
    private String defaultBranch = "main";

    @Column(name = "business_line", length = 64)
    private String businessLine;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(nullable = false, length = 20)
    private String status = "active";

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
