package com.codesec.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_history", indexes = {
    @Index(name = "idx_ticket_time", columnList = "ticket_id, operated_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketHistoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "from_status", length = 32)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 32)
    private String toStatus;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "operated_at", nullable = false, updatable = false)
    private LocalDateTime operatedAt;

    @PrePersist
    void prePersist() {
        operatedAt = LocalDateTime.now();
    }
}
