package com.codesec.api.module.webhook;

import com.codesec.api.domain.entity.RepoEntity;
import com.codesec.api.module.repo.RepoService;
import com.codesec.api.module.scan.ScanService;
import com.codesec.api.module.scan.dto.ScanCreateRequest;
import com.codesec.api.module.webhook.dto.GitlabWebhookPayload;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {
    private final RepoService repoService;
    private final ScanService scanService;

    @PostMapping("/gitlab")
    public Map<String, Object> gitlabWebhook(@RequestBody GitlabWebhookPayload payload,
                                              @RequestHeader(value = "X-Gitlab-Token", required = false) String token,
                                              HttpServletRequest request) {
        Long projectId = payload.getProjectId();
        if (projectId == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Missing project.id");
        }

        Optional<RepoEntity> repoOpt = repoService.findByGitlabProjectId(projectId);
        if (repoOpt.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Repo not found for project: " + projectId);
        }

        String commitSha = payload.getLastCommitSha();
        if (commitSha == null) {
            commitSha = "unknown";
        }

        // Create scan task
        ScanCreateRequest scanReq = new ScanCreateRequest();
        scanReq.setRepoId(repoOpt.get().getId());
        scanReq.setBranch("main");
        scanReq.setCommitSha(commitSha);
        scanReq.setMode("mr");
        var response = scanService.create(scanReq, 0L); // 0 = system account

        return Map.of(
            "status", "accepted",
            "scanId", response.getScanId(),
            "dedup", "fresh"
        );
    }
}
