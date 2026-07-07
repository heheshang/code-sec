package com.codesec.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RepoCreateRequest {
    @NotBlank private String name;
    @NotBlank private String platform = "gitlab";
    @NotBlank private String url;
    private String accessToken;
    private String webhookSecret;
    private String defaultBranch = "main";
    private String businessLine;
    private Long gitlabProjectId;
}
