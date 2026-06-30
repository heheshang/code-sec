package com.codesec.gitlab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single file change in a GitLab merge request.
 * Mirrors GitLab API response: GET /api/v4/projects/:id/merge_requests/:iid/changes
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MrChange(
    @JsonProperty("old_path") String oldPath,
    @JsonProperty("new_path") String newPath,
    @JsonProperty("new_file") boolean newFile,
    @JsonProperty("deleted_file") boolean deletedFile,
    @JsonProperty("renamed_file") boolean renamedFile,
    @JsonProperty("diff") String diff
) {
    /** Whether this change should be included in scanning. */
    public boolean shouldScan() {
        return !deletedFile;
    }

    /** The effective path for scanning (new_path for renamed files). */
    public String effectivePath() {
        return newPath != null ? newPath : oldPath;
    }
}
