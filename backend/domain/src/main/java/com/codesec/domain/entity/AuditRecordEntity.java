package com.codesec.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_record", indexes = {
    @Index(name = "idx_vuln_time", columnList = "vuln_id, audited_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditRecordEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vuln_id", nullable = false)
    private Long vulnId;

    @Column(name = "auditor_id", nullable = false)
    private Long auditorId;

    @Column(nullable = false, length = 32)
    private String action;

    @Column(name = "exploit_condition", columnDefinition = "TEXT")
    private String exploitCondition;

    @Column(name = "poc_content", columnDefinition = "TEXT")
    private String pocContent;

    @Column(name = "poc_screenshot_url", length = 512)
    private String pocScreenshotUrl;

    @Column(name = "impact_scope", length = 256)
    private String impactScope;

    @Column(name = "fix_suggestion", columnDefinition = "TEXT")
    private String fixSuggestion;

    @Column(name = "fix_code_snippet", columnDefinition = "MEDIUMTEXT")
    private String fixCodeSnippet;

    @Column(name = "audit_duration_seconds")
    private Integer auditDurationSeconds;

    @Column(name = "resulting_status", length = 32)
    private String resultingStatus;

    @Column(name = "resulting_exploitability", length = 32)
    private String resultingExploitability;

    @Column(name = "audited_at", nullable = false, updatable = false)
    private LocalDateTime auditedAt;

    @PrePersist
    void prePersist() {
        auditedAt = LocalDateTime.now();
    }
}
