package com.codesec.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rule_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleMetadataEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false, unique = true, length = 128)
    private String ruleId;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(nullable = false, length = 16)
    private String severity;

    @Column(length = 32)
    private String cwe;

    @Column(nullable = false, length = 32)
    private String language = "java";

    @Column(nullable = false, length = 32)
    private String engine = "self_sast";

    @Column(name = "detection_type", nullable = false, length = 16)
    private String detectionType = "ast";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "fix_suggestion", columnDefinition = "TEXT")
    private String fixSuggestion;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "imported_at", nullable = false, updatable = false)
    private LocalDateTime importedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (importedAt == null) importedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
