package com.codesec.api.domain.repository;

import com.codesec.api.domain.entity.ScanTaskEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ScanTaskRepository extends JpaRepository<ScanTaskEntity, Long> {
    Page<ScanTaskEntity> findByRepoIdOrderByCreatedAtDesc(Long repoId, Pageable pageable);

    Optional<ScanTaskEntity> findFirstByRepoIdAndCommitShaAndCreatedAtAfterOrderByCreatedAtDesc(
        Long repoId, String commitSha, LocalDateTime after);

    /**
     * Atomically claim the next queued scan task (PostgreSQL SKIP LOCKED).
     * Pageable(0,1) ensures LIMIT 1 so getSingleResult() never hits multiple rows.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("SELECT t FROM ScanTaskEntity t WHERE t.status = 'queued' ORDER BY t.createdAt ASC")
    Page<ScanTaskEntity> findNextQueuedTask(Pageable pageable);
}
