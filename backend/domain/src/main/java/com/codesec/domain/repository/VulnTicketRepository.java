package com.codesec.domain.repository;

import com.codesec.domain.entity.VulnTicketEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface VulnTicketRepository extends JpaRepository<VulnTicketEntity, Long> {
    Page<VulnTicketEntity> findByProjectIdAndStatus(Long projectId, String status, Pageable pageable);

    Page<VulnTicketEntity> findByStatus(String status, Pageable pageable);

    List<VulnTicketEntity> findByVulnId(Long vulnId);

    long countByStatus(String status);

    @Query("SELECT COUNT(t) FROM VulnTicketEntity t WHERE t.status = 'closed' AND t.closedAt >= ?1")
    long countClosedSince(LocalDateTime since);

    @Query("SELECT COUNT(t) FROM VulnTicketEntity t WHERE t.status = 'closed' AND t.closedAt >= ?1 AND t.closedAt < ?2")
    long countClosedBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COUNT(t) FROM VulnTicketEntity t WHERE t.createdAt >= ?1 AND t.createdAt < ?2")
    long countCreatedBetween(LocalDateTime from, LocalDateTime to);
}
