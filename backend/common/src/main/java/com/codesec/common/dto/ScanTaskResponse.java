package com.codesec.common.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ScanTaskResponse {
    private Long id;
    private Long repoId;
    private String branch;
    private String commitSha;
    private String status;
    private String engine;
    private String mode;
    private String errorMessage;
    private int findingsCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
