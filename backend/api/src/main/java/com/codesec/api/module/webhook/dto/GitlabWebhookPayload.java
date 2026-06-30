package com.codesec.api.module.webhook.dto;

import lombok.Data;
import java.util.Map;

@Data
public class GitlabWebhookPayload {
    private String objectKind;
    private Map<String, Object> project;
    private Map<String, Object> objectAttributes;

    public Long getProjectId() {
        if (project != null && project.get("id") instanceof Number n) return n.longValue();
        return null;
    }

    public String getSourceBranch() {
        if (objectAttributes != null) return (String) objectAttributes.get("source_branch");
        return null;
    }

    public String getTargetBranch() {
        if (objectAttributes != null) return (String) objectAttributes.get("target_branch");
        return null;
    }

    public String getLastCommitSha() {
        if (objectAttributes != null && objectAttributes.get("last_commit") instanceof Map<?, ?> lc) {
            return (String) lc.get("id");
        }
        return null;
    }

    public Long getMrIid() {
        if (objectAttributes != null && objectAttributes.get("iid") instanceof Number n) return n.longValue();
        return null;
    }
}
