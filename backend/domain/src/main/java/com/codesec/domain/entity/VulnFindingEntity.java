package com.codesec.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vuln_finding", indexes = {
    @Index(name = "idx_scan_task", columnList = "scan_task_id"),
    @Index(name = "idx_project_severity", columnList = "project_id, severity"),
    @Index(name = "idx_severity", columnList = "severity"),
    @Index(name = "idx_exploitability", columnList = "exploitability"),
    @Index(name = "idx_dedup_key", columnList = "dedup_key")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VulnFindingEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scan_task_id", nullable = false)
    private Long scanTaskId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "rule_id", nullable = false, length = 64)
    private String ruleId;

    @Column(nullable = false, length = 16)
    private String severity;

    @Column(nullable = false, length = 32)
    private String exploitability = "potentially_exploitable";

    @Column(nullable = false, length = 512)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "code_snippet", columnDefinition = "TEXT")
    private String codeSnippet;

    @Column(name = "file_path", nullable = false, length = 1024)
    private String filePath;

    @Column(name = "line_start", nullable = false)
    private int lineStart;

    @Column(name = "line_end", nullable = false)
    private int lineEnd;

    @Column(length = 16)
    private String cwe;

    @Column(length = 64)
    private String cve;

    @Column(nullable = false, length = 32)
    private String engine = "self_sast";

    @Column(name = "discovered_at", nullable = false)
    private LocalDateTime discoveredAt;

    @Column(name = "discovered_by", length = 64)
    private String discoveredBy;

    @Column(name = "dedup_key", length = 256)
    private String dedupKey;

    @Column(name = "ai_verdict", length = 32)
    private String aiVerdict;

    @Column(name = "ai_confidence")
    private Double aiConfidence;

    @Column(name = "ai_explanation", columnDefinition = "TEXT")
    private String aiExplanation;

    @Column(name = "ai_generated_patch", columnDefinition = "TEXT")
    private String aiGeneratedPatch;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (discoveredAt == null) discoveredAt = LocalDateTime.now();
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (dedupKey == null) dedupKey = scanTaskId + "/" + filePath + "/" + lineStart + "/" + ruleId;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
