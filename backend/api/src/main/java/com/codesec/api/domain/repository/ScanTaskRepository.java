package com.codesec.api.domain.repository;

import com.codesec.api.domain.entity.ScanTaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ScanTaskRepository extends JpaRepository<ScanTaskEntity, Long> {
    Page<ScanTaskEntity> findByRepoIdOrderByCreatedAtDesc(Long repoId, Pageable pageable);

    Optional<ScanTaskEntity> findFirstByRepoIdAndCommitShaAndCreatedAtAfterOrderByCreatedAtDesc(
        Long repoId, String commitSha, LocalDateTime after);
}
