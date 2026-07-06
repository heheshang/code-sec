package com.codesec.domain.repository;

import com.codesec.domain.entity.OperationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationLogRepository extends JpaRepository<OperationLogEntity, Long> {
}
