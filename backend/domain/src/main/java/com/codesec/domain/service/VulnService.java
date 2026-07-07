package com.codesec.domain.service;

import com.codesec.common.exception.NotFoundException;
import com.codesec.domain.entity.*;
import com.codesec.domain.repository.*;
import com.codesec.common.dto.PaginatedResult;
import com.codesec.common.dto.*;
import com.codesec.engineadapter.FindingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    /**
     * Batch-persist findings from engine scan. Called by worker after adapter maps to DTOs.
     *
     * Internal steps:
     * 1. Map FindingDto → VulnFindingEntity
     * 2. Deduplicate by dedup_key
     * 3. Insert vuln_finding rows (tsvector auto-maintained by PG trigger)
     * 4. Auto-create vuln_ticket for each (status=PENDING_AUDIT)
     *
     * @return list of persisted VulnFindingEntity records
     */
    @Transactional
    public List<VulnFindingEntity> persistBatch(List<FindingDto> findings) {
        if (findings == null || findings.isEmpty()) return List.of();

        List<VulnFindingEntity> saved = new ArrayList<>();

        for (FindingDto f : findings) {
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
                .cve(f.cve())
                .engine(f.engine() != null ? f.engine() : "self_sast")
                .aiVerdict(f.aiVerdict())
                .aiConfidence(f.aiConfidence())
                .aiExplanation(f.aiExplanation())
                .aiGeneratedPatch(f.aiGeneratedPatch())
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

        return saved;
    }

    public PaginatedResult<VulnFindingResponse> list(VulnQuery query) {
        var page = vulnRepo.findAll(PageRequest.of(query.getPage() - 1, query.getSize()));
        var items = page.getContent().stream().map(this::toResponse).toList();
        return PaginatedResult.of(items, page.getTotalElements(), query.getPage(), query.getSize());
    }

    public VulnFindingResponse getById(Long id) {
        return vulnRepo.findById(id).map(this::toResponse)
            .orElseThrow(() -> new NotFoundException("Vuln finding not found: " + id));
    }

    public long countBySeverity(String severity) { return vulnRepo.countBySeverity(severity); }
    public long countAll() { return vulnRepo.countAll(); }

    private VulnFindingResponse toResponse(VulnFindingEntity e) {
        return VulnFindingResponse.builder()
            .id(e.getId()).scanTaskId(e.getScanTaskId()).projectId(e.getProjectId())
            .ruleId(e.getRuleId()).severity(e.getSeverity()).exploitability(e.getExploitability())
            .title(e.getTitle()).description(e.getDescription()).codeSnippet(e.getCodeSnippet())
            .filePath(e.getFilePath()).lineStart(e.getLineStart()).lineEnd(e.getLineEnd())
            .cwe(e.getCwe()).cve(e.getCve()).engine(e.getEngine()).discoveredAt(e.getDiscoveredAt())
            .aiVerdict(e.getAiVerdict()).aiConfidence(e.getAiConfidence())
            .aiExplanation(e.getAiExplanation()).aiGeneratedPatch(e.getAiGeneratedPatch())
            .build();
    }

    private String buildDedupKey(FindingDto f) {
        return f.scanId() + "/" + f.filePath() + "/" + f.lineStart() + "/" + f.ruleId();
    }

    private static Long parseLongOrNull(String s) {
        if (s == null) return null;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }
}
