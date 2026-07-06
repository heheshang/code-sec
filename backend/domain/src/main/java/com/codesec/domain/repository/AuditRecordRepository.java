package com.codesec.domain.repository;

import com.codesec.domain.entity.AuditRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditRecordRepository extends JpaRepository<AuditRecordEntity, Long> {
    List<AuditRecordEntity> findByVulnIdOrderByAuditedAtDesc(Long vulnId);
}
