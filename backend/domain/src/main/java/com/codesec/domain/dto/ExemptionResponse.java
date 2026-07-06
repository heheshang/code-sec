package com.codesec.domain.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ExemptionResponse {
    private Long id;
    private Long projectId;
    private Long ruleId;
    private String ruleName;
    private String ruleSeverity;
    private String reason;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
