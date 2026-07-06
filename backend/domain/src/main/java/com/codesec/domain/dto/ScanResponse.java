package com.codesec.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScanResponse {
    private Long scanId;
    private String status;
    private int estimatedDurationSeconds;
}
