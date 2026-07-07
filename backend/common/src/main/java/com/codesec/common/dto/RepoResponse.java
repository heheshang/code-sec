package com.codesec.common.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RepoResponse {
    private Long id;
    private String name;
    private String platform;
    private String url;
    private String defaultBranch;
    private String businessLine;
    private String status;
    private Long gitlabProjectId;
    private boolean hasToken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
