package com.codesec.domain.service.audit;

import com.codesec.domain.entity.*;
import com.codesec.domain.repository.*;
import com.codesec.domain.dto.*;
import com.codesec.domain.service.ticket.statemachine.TicketStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditRecordRepository auditRepo;
    private final VulnFindingRepository vulnRepo;
    private final VulnTicketRepository ticketRepo;
    private final TicketHistoryRepository ticketHistoryRepo;
    private final UserRepository userRepo;

    @Transactional
    public AuditResponse submitAudit(AuditSubmitRequest req, Long auditorId) {
        VulnFindingEntity vuln = vulnRepo.findById(req.getVulnId())
            .orElseThrow(() -> new RuntimeException("Vuln not found"));

        List<VulnTicketEntity> tickets = ticketRepo.findByVulnId(req.getVulnId());
        VulnTicketEntity ticket = tickets.isEmpty() ? null : tickets.get(0);

        String targetStatus = switch (req.getAction()) {
            case "confirm" -> "confirmed";
            case "false_positive" -> "false_positive";
            case "need_retest" -> "pending_retest";
            default -> throw new RuntimeException("Invalid audit action: " + req.getAction());
        };

        if (ticket != null) {
            TicketStateMachine.assertValid(ticket.getStatus(), targetStatus, "auditor");

            // Record ticket history
            TicketHistoryEntity history = TicketHistoryEntity.builder()
                .ticketId(ticket.getId())
                .fromStatus(ticket.getStatus())
                .toStatus(targetStatus)
                .operatorId(auditorId)
                .comment("Audit: " + req.getAction())
                .build();
            ticketHistoryRepo.save(history);

            // Update ticket
            ticket.setStatus(targetStatus);
            if ("false_positive".equals(req.getAction())) {
                ticket.setClosedAt(LocalDateTime.now());
            }
            ticketRepo.save(ticket);
        }

        // Create audit record (OP-2: do NOT overwrite vuln_finding.exploitability)
        AuditRecordEntity audit = AuditRecordEntity.builder()
            .vulnId(req.getVulnId())
            .auditorId(auditorId)
            .action(req.getAction())
            .exploitCondition(req.getExploitCondition())
            .pocContent(req.getPocContent())
            .impactScope(req.getImpactScope())
            .fixSuggestion(req.getFixSuggestion())
            .fixCodeSnippet(req.getFixCodeSnippet())
            .resultingStatus(targetStatus)
            .resultingExploitability("false_positive".equals(req.getAction()) ? "not_exploitable" : "exploitable")
            .build();

        audit = auditRepo.save(audit);
        return toResponse(audit);
    }

    public List<AuditResponse> getAuditHistory(Long vulnId) {
        return auditRepo.findByVulnIdOrderByAuditedAtDesc(vulnId)
            .stream().map(this::toResponse).toList();
    }

    private AuditResponse toResponse(AuditRecordEntity e) {
        String auditorName = userRepo.findById(e.getAuditorId())
            .map(UserEntity::getUsername).orElse("unknown");
        return AuditResponse.builder()
            .id(e.getId()).vulnId(e.getVulnId()).auditorId(e.getAuditorId())
            .auditorName(auditorName).action(e.getAction())
            .exploitCondition(e.getExploitCondition()).pocContent(e.getPocContent())
            .impactScope(e.getImpactScope()).fixSuggestion(e.getFixSuggestion())
            .fixCodeSnippet(e.getFixCodeSnippet())
            .resultingStatus(e.getResultingStatus())
            .resultingExploitability(e.getResultingExploitability())
            .auditedAt(e.getAuditedAt())
            .build();
    }
}
