package com.codesec.api.module.vuln;

import com.codesec.api.domain.entity.*;
import com.codesec.api.domain.repository.*;
import com.codesec.api.interfaces.dto.PaginatedResult;
import com.codesec.api.module.vuln.dto.*;
import com.codesec.api.application.event.VulnIndexedEvent;
import com.codesec.engine.model.Finding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class VulnService {
    private final VulnFindingRepository vulnRepo;
    private final VulnTicketRepository ticketRepo;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Batch-persist findings from engine scan. Called synchronously by EngineAdapterImpl.
     *
     * Internal steps:
     * 1. Map Finding → VulnFindingEntity
     * 2. Deduplicate by dedup_key
     * 3. Insert vuln_finding rows
     * 4. Auto-create vuln_ticket for each (status=PENDING_AUDIT)
     * 5. Publish VulnIndexedEvent for ES indexing
     *
     * @return list of persisted VulnFindingEntity records
     */
    @Transactional
    public List<VulnFindingEntity> persistBatch(List<Finding> findings) {
        if (findings == null || findings.isEmpty()) return List.of();

        List<VulnFindingEntity> saved = new ArrayList<>();

        for (Finding f : findings) {
            String dedupKey = buildDedupKey(f);
            if (vulnRepo.findByDedupKey(dedupKey).isPresent()) {
                log.debug("Skipping duplicate finding: {}", dedupKey);
                continue;
            }

            String severity = f.severity() != null ? f.severity() : "medium";

            VulnFindingEntity entity = VulnFindingEntity.builder()
                .scanTaskId(parseLongOrNull(f.scanId()))
                .projectId(f.projectId() != null ? f.projectId().longValue() : 0L)
                .ruleId(f.ruleId())
                .severity(severity)
                .exploitability(f.exploitability() != null ? f.exploitability() : "potentially_exploitable")
                .title(f.title())
                .description(f.description())
                .codeSnippet(f.codeSnippet())
                .filePath(f.filePath())
                .lineStart(f.lineStart())
                .lineEnd(f.lineEnd())
                .cwe(f.cwe())
                .engine(f.engine() != null ? f.engine() : "self_sast")
                .discoveredAt(f.discoveredAt() != null ?
                    LocalDateTime.ofInstant(f.discoveredAt(), ZoneOffset.UTC) : LocalDateTime.now())
                .discoveredBy("engine")
                .dedupKey(dedupKey)
                .build();

            entity = vulnRepo.save(entity);
            saved.add(entity);

            // Auto-create ticket
            VulnTicketEntity ticket = VulnTicketEntity.builder()
                .vulnId(entity.getId())
                .projectId(entity.getProjectId())
                .status("pending_audit")
                .severity(severity)
                .build();
            ticketRepo.save(ticket);
        }

        log.info("Persisted {} new findings ({} deduplicated)", saved.size(), findings.size() - saved.size());

        // Publish event for ES indexing (in-process, synchronous)
        eventPublisher.publishEvent(new VulnIndexedEvent(this, findings));

        return saved;
    }

    public PaginatedResult<VulnFindingResponse> list(VulnQuery query) {
        var page = vulnRepo.findAll(PageRequest.of(query.getPage() - 1, query.getSize()));
        var items = page.getContent().stream().map(this::toResponse).toList();
        return PaginatedResult.of(items, page.getTotalElements(), query.getPage(), query.getSize());
    }

    public VulnFindingResponse getById(Long id) {
        return vulnRepo.findById(id).map(this::toResponse)
            .orElseThrow(() -> new RuntimeException("Vuln finding not found: " + id));
    }

    public long countBySeverity(String severity) { return vulnRepo.countBySeverity(severity); }
    public long countAll() { return vulnRepo.countAll(); }

    private VulnFindingResponse toResponse(VulnFindingEntity e) {
        return VulnFindingResponse.builder()
            .id(e.getId()).scanTaskId(e.getScanTaskId()).projectId(e.getProjectId())
            .ruleId(e.getRuleId()).severity(e.getSeverity()).exploitability(e.getExploitability())
            .title(e.getTitle()).description(e.getDescription()).codeSnippet(e.getCodeSnippet())
            .filePath(e.getFilePath()).lineStart(e.getLineStart()).lineEnd(e.getLineEnd())
            .cwe(e.getCwe()).engine(e.getEngine()).discoveredAt(e.getDiscoveredAt())
            .build();
    }

    private String buildDedupKey(Finding f) {
        return f.scanId() + "/" + f.filePath() + "/" + f.lineStart() + "/" + f.ruleId();
    }

    private static Long parseLongOrNull(String s) {
        if (s == null) return null;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }
}
