package com.codesec.api.domain.repository;

import com.codesec.api.domain.entity.VulnFindingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface VulnFindingRepository extends JpaRepository<VulnFindingEntity, Long>, JpaSpecificationExecutor<VulnFindingEntity> {
    Page<VulnFindingEntity> findByScanTaskId(Long scanTaskId, Pageable pageable);

    Optional<VulnFindingEntity> findByDedupKey(String dedupKey);

    @Query("SELECT COUNT(v) FROM VulnFindingEntity v")
    long countAll();

    @Query("SELECT COUNT(v) FROM VulnFindingEntity v WHERE v.severity = ?1")
    long countBySeverity(String severity);
}
