package com.codesec.domain.service.ticket;

import com.codesec.common.exception.NotFoundException;
import com.codesec.domain.entity.*;
import com.codesec.domain.repository.*;
import com.codesec.common.dto.*;
import com.codesec.common.dto.PaginatedResult;
import com.codesec.domain.service.ticket.statemachine.TicketStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final VulnTicketRepository ticketRepo;
    private final TicketHistoryRepository historyRepo;
    private final UserRepository userRepo;

    public PaginatedResult<TicketResponse> list(String status, int page, int size) {
        var p = (status != null && !status.isEmpty())
            ? ticketRepo.findByStatus(status, PageRequest.of(page - 1, size))
            : ticketRepo.findAll(PageRequest.of(page - 1, size));

        // Batch-resolve assignee names: collect unique assignee IDs, query once
        java.util.Set<Long> assigneeIds = p.getContent().stream()
            .map(VulnTicketEntity::getAssigneeId)
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());
        java.util.Map<Long, String> userNameMap = userRepo.findAllById(assigneeIds).stream()
            .collect(java.util.stream.Collectors.toMap(UserEntity::getId, UserEntity::getUsername));

        var items = p.getContent().stream().map(t -> {
            String name = t.getAssigneeId() != null ? userNameMap.get(t.getAssigneeId()) : null;
            return toResponse(t, name);
        }).toList();
        return PaginatedResult.of(items, p.getTotalElements(), page, size);
    }

    public TicketResponse getById(Long id) {
        return ticketRepo.findById(id).map(t -> {
            String name = resolveAssigneeName(t.getAssigneeId());
            return toResponse(t, name);
        }).orElseThrow(() -> new NotFoundException("Ticket not found: " + id));
    }

    public List<TicketHistoryItem> getHistory(Long ticketId) {
        return historyRepo.findByTicketIdOrderByOperatedAtDesc(ticketId)
            .stream().map(h -> TicketHistoryItem.builder()
                .id(h.getId()).fromStatus(h.getFromStatus()).toStatus(h.getToStatus())
                .comment(h.getComment()).operatorId(h.getOperatorId()).operatedAt(h.getOperatedAt())
                .build()).toList();
    }

    @Transactional
    public TicketResponse transition(Long ticketId, TicketTransitionRequest req, Long operatorId) {
        VulnTicketEntity ticket = ticketRepo.findById(ticketId)
            .orElseThrow(() -> new NotFoundException("Ticket not found: " + ticketId));

        TicketStateMachine.assertValid(ticket.getStatus(), req.getToStatus(), "operator");

        TicketHistoryEntity history = TicketHistoryEntity.builder()
            .ticketId(ticketId)
            .fromStatus(ticket.getStatus())
            .toStatus(req.getToStatus())
            .operatorId(operatorId)
            .comment(req.getComment())
            .build();
        historyRepo.save(history);

        ticket.setStatus(req.getToStatus());
        if (req.getAssigneeId() != null) ticket.setAssigneeId(req.getAssigneeId());
        if ("closed".equals(req.getToStatus())) ticket.setClosedAt(LocalDateTime.now());
        ticket = ticketRepo.save(ticket);

        String name = resolveAssigneeName(ticket.getAssigneeId());
        return toResponse(ticket, name);
    }

    public long countByStatus(String status) { return ticketRepo.countByStatus(status); }

    private String resolveAssigneeName(Long assigneeId) {
        if (assigneeId == null) return null;
        return userRepo.findAllById(java.util.Set.of(assigneeId)).stream()
            .findFirst().map(UserEntity::getUsername).orElse(null);
    }

    private TicketResponse toResponse(VulnTicketEntity t, String assigneeName) {
        return TicketResponse.builder()
            .id(t.getId()).vulnId(t.getVulnId()).projectId(t.getProjectId())
            .status(t.getStatus()).severity(t.getSeverity())
            .assigneeId(t.getAssigneeId()).assigneeName(assigneeName)
            .deadline(t.getDeadline()).createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt())
            .build();
    }
}
