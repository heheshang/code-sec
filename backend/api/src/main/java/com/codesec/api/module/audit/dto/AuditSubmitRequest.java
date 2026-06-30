package com.codesec.api.module.audit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class AuditSubmitRequest {
    @NotNull private Long vulnId;
    @NotBlank private String action;
    private String exploitCondition;
    private String pocContent;
    private List<String> pocAttachments;
    private String impactScope;
    private String businessScenario;
    private String fixSuggestion;
    private String fixCodeSnippet;
    private String fixLanguage;
}
