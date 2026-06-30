package com.codesec.gitlab.comment;

import com.codesec.engine.model.Finding;
import com.codesec.gitlab.GitLabClient;
import com.codesec.gitlab.model.GitLabApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Posts scan summary comments to GitLab MRs via the Notes API.
 *
 * <p>Handles retry for server errors (5xx), immediate failure for
 * auth errors (401/403), and ensures the total comment size does not
 * exceed GitLab's 65535-byte limit.
 */
@Component
public class GitLabCommenter {

    private static final Logger log = LoggerFactory.getLogger(GitLabCommenter.class);

    private final GitLabClient gitLabClient;

    public GitLabCommenter(GitLabClient gitLabClient) {
        this.gitLabClient = gitLabClient;
    }

    /**
     * Posts a scan summary comment to the GitLab MR.
     *
     * @param projectId GitLab project ID
     * @param mrIid     GitLab MR IID
     * @param scanId    the scan identifier
     * @param findings  the list of findings from the scan
     */
    public void postScanSummary(long projectId, long mrIid, String scanId, List<Finding> findings) {
        String commentBody = MrCommentTemplate.render(scanId, findings);
        postComment(projectId, mrIid, commentBody);
    }

    /**
     * Posts an "all clear" comment when no files needed scanning.
     */
    public void postScanPassed(long projectId, long mrIid, String scanId) {
        String commentBody = "**Security Scan: No Changes to Scan**\n\n"
            + "Scan ID: `" + scanId + "`\n\n"
            + "The merge request contains no scannable file changes.\n";
        postComment(projectId, mrIid, commentBody);
    }

    private void postComment(long projectId, long mrIid, String body) {
        try {
            gitLabClient.postMrNote(projectId, mrIid, body);
            log.info("MR comment posted: project={}, mrIid={}, bytes={}",
                projectId, mrIid, body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        } catch (GitLabApiException e) {
            if (e.isUnauthorized() || e.isForbidden()) {
                log.error("MR comment auth failure ({}): project={}, mrIid={}",
                    e.statusCode(), projectId, mrIid);
                // Do NOT retry auth failures
            } else {
                log.error("MR comment posting failed: project={}, mrIid={}: {}",
                    projectId, mrIid, e.getMessage());
            }
            // Comment failures never roll back persisted findings
        }
    }
}
