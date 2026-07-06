package com.codesec.domain.dto;

import lombok.Data;

@Data
public class RuleUpdateRequest {
    private Boolean enabled;
    private String severity;
    private String description;
}
