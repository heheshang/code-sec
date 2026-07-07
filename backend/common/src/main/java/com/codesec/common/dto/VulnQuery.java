package com.codesec.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VulnQuery {
    private String severity;
    private String status;
    private String exploitability;
    private Long projectId;
    private Long scanTaskId;
    private int page = 1;
    private int size = 20;
}
