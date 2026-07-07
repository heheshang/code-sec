package com.codesec.common.dto;

import lombok.Data;

@Data
public class RepoUpdateRequest {
    private String name;
    private String url;
    private String accessToken;
    private String defaultBranch;
    private String businessLine;
    private String status;
}
