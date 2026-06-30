package com.codesec.api.domain.repository;

import com.codesec.api.domain.entity.OperationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationLogRepository extends JpaRepository<OperationLogEntity, Long> {
}
