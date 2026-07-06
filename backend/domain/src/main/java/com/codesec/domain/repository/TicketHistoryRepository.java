package com.codesec.domain.repository;

import com.codesec.domain.entity.TicketHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistoryEntity, Long> {
    List<TicketHistoryEntity> findByTicketIdOrderByOperatedAtDesc(Long ticketId);
}
