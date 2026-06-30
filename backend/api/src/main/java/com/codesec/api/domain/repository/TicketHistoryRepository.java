package com.codesec.api.domain.repository;

import com.codesec.api.domain.entity.TicketHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistoryEntity, Long> {
    List<TicketHistoryEntity> findByTicketIdOrderByOperatedAtDesc(Long ticketId);
}
