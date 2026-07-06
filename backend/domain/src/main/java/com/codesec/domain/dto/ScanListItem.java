package com.codesec.domain.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ScanListItem {
    private Long id;
    private Long repoId;
    private String branch;
    private String status;
    private String mode;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
