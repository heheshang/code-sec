package com.codesec.api.module.ticket.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketResponse {
    private Long id;
    private Long vulnId;
    private Long projectId;
    private String status;
    private String severity;
    private Long assigneeId;
    private String assigneeName;
    private LocalDate deadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
