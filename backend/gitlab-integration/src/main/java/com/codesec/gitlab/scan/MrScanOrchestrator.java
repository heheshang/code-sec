package com.codesec.gitlab.scan;

import com.codesec.engine.model.Finding;
import com.codesec.gitlab.GitLabClient;
import com.codesec.gitlab.comment.GitLabCommenter;
import com.codesec.gitlab.dev.DevEngineAdapter;
import com.codesec.gitlab.dev.DevRepoService;
import com.codesec.gitlab.dev.DevVulnService;
import com.codesec.gitlab.model.GitLabApiException;
import com.codesec.gitlab.model.MrChangesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the full MR scan pipeline.
 *
 * <p>Pipeline: GitLab MR Changes API -> DiffExtractor -> EngineAdapter.scanFiles
 * -> VulnService.persistBatch -> GitLabCommenter.postSummary
 *
 * <p>Each stage is independently guarded: a failure in one stage
 * does not roll back completed stages.
 */
@Component
public class MrScanOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(MrScanOrchestrator.class);

    private final GitLabClient gitLabClient;
    private final DevEngineAdapter engineAdapter;
    private final DevRepoService repoService;
    private final DevVulnService vulnService;
    private final GitLabCommenter commenter;

    public MrScanOrchestrator(GitLabClient gitLabClient,
                              DevEngineAdapter engineAdapter,
                              DevRepoService repoService,
                              DevVulnService vulnService,
                              GitLabCommenter commenter) {
        this.gitLabClient = gitLabClient;
        this.engineAdapter = engineAdapter;
        this.repoService = repoService;
        this.vulnService = vulnService;
        this.commenter = commenter;
    }

    /**
     * Executes the full scan pipeline for a given MR.
     *
     * @param projectId GitLab project ID
     * @param mrIid     GitLab MR IID
     */
    public void orchestrate(long projectId, long mrIid) {
        String scanId = "scan-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("MR scan orchestration started: project={}, mrIid={}, scanId={}",
            projectId, mrIid, scanId);

        try {
            // Stage 1: Fetch MR Changes
            MrChangesResponse changes;
            try {
                changes = gitLabClient.getMrChanges(projectId, mrIid);
                int changeCount = changes.changes() != null ? changes.changes().size() : 0;
                log.info("Fetched MR changes: {} files", changeCount);
            } catch (GitLabApiException e) {
                log.error("Failed to fetch MR changes for project={}, mrIid={}: {}",
                    projectId, mrIid, e.getMessage());
                return;
            }

            // Stage 2: Extract files to scan
            MrDiffResult diffResult = DiffExtractor.extract(changes);
            if (diffResult.truncated()) {
                log.warn("MR diff truncated to {} files (total: {})",
                    DiffExtractor.MAX_FILES, diffResult.totalFiles());
            }

            if (diffResult.isEmpty()) {
                log.info("MR diff is empty, skipping scan");
                commenter.postScanPassed(projectId, mrIid, scanId);
                return;
            }

            log.info("Files to scan: {}", diffResult.relativeFiles().size());

            // Stage 3: Checkout + Engine scan
            List<Finding> findings;
            try {
                findings = engineAdapter.scanFiles(diffResult.relativeFiles());
            } catch (Exception e) {
                log.error("Engine scan failed: {}", e.getMessage(), e);
                return;
            }

            // Stage 4: Persist findings
            int persistedCount = 0;
            if (!findings.isEmpty()) {
                try {
                    persistedCount = vulnService.persistBatch(scanId, findings);
                    log.info("Persisted {} findings for scanId={}", persistedCount, scanId);
                } catch (Exception e) {
                    log.error("Failed to persist findings: {}", e.getMessage(), e);
                }
            }

            // Stage 5: Post MR comment
            try {
                commenter.postScanSummary(projectId, mrIid, scanId, findings);
            } catch (Exception e) {
                log.error("Failed to post MR comment: {}", e.getMessage(), e);
            }

            log.info("MR scan orchestration complete: scanId={}, findings={}, persisted={}",
                scanId, findings.size(), persistedCount);

        } catch (Exception e) {
            log.error("MR scan orchestration failed unexpectedly: {}", e.getMessage(), e);
        }
    }
}
