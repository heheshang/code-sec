package com.codesec.domain.service.scan;

import com.codesec.common.exception.NotFoundException;
import com.codesec.domain.entity.*;
import com.codesec.domain.repository.*;
import com.codesec.domain.dto.*;
import com.codesec.domain.dto.PaginatedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ScanService {
    private final ScanTaskRepository scanRepo;
    private final VulnFindingRepository vulnRepo;

    @Transactional
    public ScanResponse create(ScanCreateRequest req, Long createdBy) {
        // Idempotency: same repo_id + commit_sha within 30s
        if (req.getCommitSha() != null) {
            LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);
            var existing = scanRepo.findFirstByRepoIdAndCommitShaAndCreatedAtAfterOrderByCreatedAtDesc(
                req.getRepoId(), req.getCommitSha(), threshold);
            if (existing.isPresent()) {
                return ScanResponse.builder()
                    .scanId(existing.get().getId()).status("queued")
                    .estimatedDurationSeconds(180).build();
            }
        }

        ScanTaskEntity task = ScanTaskEntity.builder()
            .repoId(req.getRepoId())
            .branch(req.getBranch())
            .commitSha(req.getCommitSha())
            .mode(req.getMode())
            .engine(req.getEngines() != null && !req.getEngines().isEmpty() ? req.getEngines().get(0) : "self_sast")
            .status("queued")
            .createdBy(createdBy)
            .build();

        task = scanRepo.save(task);

        return ScanResponse.builder()
            .scanId(task.getId()).status("queued")
            .estimatedDurationSeconds(180).build();
    }

    public ScanTaskResponse getById(Long id) {
        return scanRepo.findById(id).map(task -> {
            int count = (int) vulnRepo.findByScanTaskId(task.getId(), PageRequest.of(0, 1)).getTotalElements();
            return ScanTaskResponse.builder()
                .id(task.getId()).repoId(task.getRepoId()).branch(task.getBranch())
                .commitSha(task.getCommitSha()).status(task.getStatus())
                .engine(task.getEngine()).mode(task.getMode())
                .errorMessage(task.getErrorMessage())
                .findingsCount(count)
                .startedAt(task.getStartedAt()).finishedAt(task.getFinishedAt())
                .build();
        }).orElseThrow(() -> new NotFoundException("Scan task not found: " + id));
    }

    public PaginatedResult<ScanListItem> list(Long repoId, int page, int size) {
        if (repoId == null) {
            return PaginatedResult.of(List.of(), 0, page, size);
        }
        var p = scanRepo.findByRepoIdOrderByCreatedAtDesc(repoId, PageRequest.of(page - 1, size));
        var items = p.getContent().stream().map(t -> ScanListItem.builder()
            .id(t.getId()).repoId(t.getRepoId()).branch(t.getBranch())
            .status(t.getStatus()).mode(t.getMode())
            .startedAt(t.getStartedAt()).finishedAt(t.getFinishedAt()).build()
        ).toList();
        return PaginatedResult.of(items, p.getTotalElements(), page, size);
    }

    @Transactional
    public void cancel(Long id) {
        scanRepo.findById(id).ifPresent(t -> {
            if (t.getStatus().equals("queued")) {
                t.setStatus("canceled");
                scanRepo.save(t);
            }
        });
    }
}
