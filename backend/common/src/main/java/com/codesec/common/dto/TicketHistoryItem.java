package com.codesec.common.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketHistoryItem {
    private Long id;
    private String fromStatus;
    private String toStatus;
    private String comment;
    private Long operatorId;
    private LocalDateTime operatedAt;
}
