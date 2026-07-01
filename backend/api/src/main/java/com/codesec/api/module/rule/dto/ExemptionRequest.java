package com.codesec.api.module.rule.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExemptionRequest {
    private Long ruleId;
    private String reason;
    private LocalDateTime expiresAt;
}
