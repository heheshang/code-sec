package com.codesec.domain.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RuleResponse {
    private Long id;
    private String ruleId;
    private String name;
    private String severity;
    private String cwe;
    private String language;
    private String engine;
    private String detectionType;
    private String description;
    private String fixSuggestion;
    private Boolean enabled;
    private LocalDateTime importedAt;
    private LocalDateTime updatedAt;
}
