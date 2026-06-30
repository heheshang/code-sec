package com.codesec.api.module.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketTransitionRequest {
    @NotBlank private String toStatus;
    private String comment;
    private Long assigneeId;
}
