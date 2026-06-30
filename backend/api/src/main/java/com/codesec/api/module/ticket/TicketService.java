package com.codesec.api.module.ticket;

import com.codesec.api.domain.entity.*;
import com.codesec.api.domain.repository.*;
import com.codesec.api.module.ticket.dto.*;
import com.codesec.api.interfaces.dto.PaginatedResult;
import com.codesec.api.module.ticket.statemachine.TicketStateMachine;
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
        var items = p.getContent().stream().map(t -> {
            String name = t.getAssigneeId() != null
                ? userRepo.findById(t.getAssigneeId()).map(UserEntity::getUsername).orElse(null) : null;
            return toResponse(t, name);
        }).toList();
        return PaginatedResult.of(items, p.getTotalElements(), page, size);
    }

    public TicketResponse getById(Long id) {
        return ticketRepo.findById(id).map(t -> {
            String name = t.getAssigneeId() != null
                ? userRepo.findById(t.getAssigneeId()).map(UserEntity::getUsername).orElse(null) : null;
            return toResponse(t, name);
        }).orElseThrow(() -> new RuntimeException("Ticket not found: " + id));
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
            .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

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

        String name = ticket.getAssigneeId() != null
            ? userRepo.findById(ticket.getAssigneeId()).map(UserEntity::getUsername).orElse(null) : null;
        return toResponse(ticket, name);
    }

    public long countByStatus(String status) { return ticketRepo.countByStatus(status); }

    private TicketResponse toResponse(VulnTicketEntity t, String assigneeName) {
        return TicketResponse.builder()
            .id(t.getId()).vulnId(t.getVulnId()).projectId(t.getProjectId())
            .status(t.getStatus()).severity(t.getSeverity())
            .assigneeId(t.getAssigneeId()).assigneeName(assigneeName)
            .deadline(t.getDeadline()).createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt())
            .build();
    }
}
