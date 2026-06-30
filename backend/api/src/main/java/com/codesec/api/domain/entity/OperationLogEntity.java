package com.codesec.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_log", indexes = {
    @Index(name = "idx_user_time", columnList = "user_id, operated_at"),
    @Index(name = "idx_resource", columnList = "resource_type, resource_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationLogEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 32)
    private String resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "operated_at", nullable = false, updatable = false)
    private LocalDateTime operatedAt;

    @PrePersist
    void prePersist() {
        operatedAt = LocalDateTime.now();
    }
}
