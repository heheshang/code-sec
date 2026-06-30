package com.codesec.api.module.ticket;

import com.codesec.api.module.ticket.dto.*;
import com.codesec.api.interfaces.dto.PaginatedResult;
import com.codesec.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @GetMapping
    @PreAuthorize("@perm.check('ticket:read')")
    public PaginatedResult<TicketResponse> list(@RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return ticketService.list(status, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.check('ticket:read')")
    public TicketResponse get(@PathVariable Long id) {
        return ticketService.getById(id);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("@perm.check('ticket:read')")
    public List<TicketHistoryItem> history(@PathVariable Long id) {
        return ticketService.getHistory(id);
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("@perm.check('ticket:assign')")
    public TicketResponse transition(@PathVariable Long id, @RequestBody TicketTransitionRequest req,
                                      @AuthenticationPrincipal UserPrincipal user) {
        return ticketService.transition(id, req, user.getUserId());
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("@perm.check('ticket:assign')")
    public TicketResponse assign(@PathVariable Long id, @RequestBody TicketTransitionRequest req,
                                  @AuthenticationPrincipal UserPrincipal user) {
        return ticketService.transition(id, req, user.getUserId());
    }

    @PostMapping("/{id}/waive-rejected")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public Map<String, String> waive() {
        return Map.of("message", "WAIVED state deferred to Sprint 3");
    }
}
