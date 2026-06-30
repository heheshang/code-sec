package com.codesec.api.module.audit.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AuditResponse {
    private Long id;
    private Long vulnId;
    private Long auditorId;
    private String auditorName;
    private String action;
    private String exploitCondition;
    private String pocContent;
    private String pocScreenshotUrl;
    private String impactScope;
    private String fixSuggestion;
    private String fixCodeSnippet;
    private String resultingStatus;
    private String resultingExploitability;
    private LocalDateTime auditedAt;
}
