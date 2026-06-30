package com.codesec.api.module.vuln.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class VulnListItem {
    private Long id;
    private Long projectId;
    private String ruleId;
    private String severity;
    private String exploitability;
    private String title;
    private String filePath;
    private int lineStart;
    private String cwe;
    private LocalDateTime discoveredAt;
}
