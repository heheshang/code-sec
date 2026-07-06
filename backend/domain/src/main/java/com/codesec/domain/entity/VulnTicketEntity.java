package com.codesec.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vuln_ticket", indexes = {
    @Index(name = "idx_assignee_status", columnList = "assignee_id, status"),
    @Index(name = "idx_project_status", columnList = "project_id, status"),
    @Index(name = "idx_vuln_id", columnList = "vuln_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VulnTicketEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vuln_id", nullable = false)
    private Long vulnId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 32)
    private String status = "pending_scan";

    @Column(nullable = false, length = 16)
    private String severity;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "reporter_id")
    private Long reporterId;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "fixed_at")
    private LocalDateTime fixedAt;

    @Column(name = "retest_at")
    private LocalDateTime retestAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "waiver_reason", length = 512)
    private String waiverReason;

    @Column(name = "waiver_approver_id")
    private Long waiverApproverId;

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
