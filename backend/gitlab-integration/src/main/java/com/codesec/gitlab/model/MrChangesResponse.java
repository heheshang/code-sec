package com.codesec.gitlab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response from GitLab MR Changes API.
 * GET /api/v4/projects/:id/merge_requests/:iid/changes
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MrChangesResponse(
    @JsonProperty("id") Long id,
    @JsonProperty("iid") Long iid,
    @JsonProperty("title") String title,
    @JsonProperty("source_branch") String sourceBranch,
    @JsonProperty("target_branch") String targetBranch,
    @JsonProperty("changes") List<MrChange> changes,
    @JsonProperty("sha") String sha,
    @JsonProperty("diff_refs") DiffRefs diffRefs
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DiffRefs(
        @JsonProperty("base_sha") String baseSha,
        @JsonProperty("head_sha") String headSha,
        @JsonProperty("start_sha") String startSha
    ) {}
}
