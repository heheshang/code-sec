package com.codesec.gitlab.webhook;

import com.codesec.gitlab.scan.MrScanOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handles GitLab Merge Request webhook events.
 *
 * <p>Extracts MR metadata from the webhook payload and triggers
 * the MR scan orchestration pipeline.
 */
@Component
public class MergeRequestEventHandler {

    private static final Logger log = LoggerFactory.getLogger(MergeRequestEventHandler.class);

    private final MrScanOrchestrator scanOrchestrator;

    public MergeRequestEventHandler(MrScanOrchestrator scanOrchestrator) {
        this.scanOrchestrator = scanOrchestrator;
    }

    /**
     * Processes a Merge Request webhook payload.
     *
     * @param payload the parsed JSON webhook body
     */
    public void handle(Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) payload.get("project");
        @SuppressWarnings("unchecked")
        Map<String, Object> objectAttributes =
            (Map<String, Object>) payload.get("object_attributes");

        if (project == null || objectAttributes == null) {
            log.warn("MR webhook payload missing 'project' or 'object_attributes'");
            return;
        }

        long projectId = ((Number) project.get("id")).longValue();
        long mrIid = ((Number) objectAttributes.get("iid")).longValue();
        String action = (String) objectAttributes.get("action");
        String sourceBranch = (String) objectAttributes.get("source_branch");
        String targetBranch = (String) objectAttributes.get("target_branch");

        log.info("Processing MR webhook: project={}, mrIid={}, action={}, {} → {}",
            projectId, mrIid, action, sourceBranch, targetBranch);

        try {
            scanOrchestrator.orchestrate(projectId, mrIid);
        } catch (Exception e) {
            log.error("MR scan orchestration failed for project={}, mrIid={}: {}",
                projectId, mrIid, e.getMessage(), e);
        }
    }
}
