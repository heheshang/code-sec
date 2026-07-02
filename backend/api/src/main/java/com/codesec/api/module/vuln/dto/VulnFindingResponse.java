package com.codesec.api.module.vuln.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class VulnFindingResponse {
    private Long id;
    private Long scanTaskId;
    private Long projectId;
    private String ruleId;
    private String severity;
    private String exploitability;
    private String title;
    private String description;
    private String codeSnippet;
    private String filePath;
    private int lineStart;
    private int lineEnd;
    private String cwe;
    private String cve;
    private String engine;
    private LocalDateTime discoveredAt;
}
