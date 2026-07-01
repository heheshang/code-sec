package com.codesec.api.module.rule.dto;

import lombok.Data;

@Data
public class RuleUpdateRequest {
    private Boolean enabled;
    private String severity;
    private String description;
}
