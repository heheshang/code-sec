package com.codesec.domain.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RepoListItem {
    private Long id;
    private String name;
    private String platform;
    private String url;
    private String businessLine;
    private String status;
    private LocalDateTime createdAt;
}
