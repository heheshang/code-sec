package com.codesec.api.module.webhook;

import com.codesec.common.crypto.CryptoService;
import com.codesec.domain.entity.RepoEntity;
import com.codesec.domain.service.repo.RepoService;
import com.codesec.domain.service.scan.ScanService;
import com.codesec.domain.dto.ScanCreateRequest;
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
    private final CryptoService cryptoService;

    @PostMapping("/gitlab")
    public Map<String, Object> gitlabWebhook(@RequestBody GitlabWebhookPayload payload,
                                              @RequestHeader(value = "X-Gitlab-Token", required = false) String token,
                                              HttpServletRequest request) {
        // Validate webhook token against the repo's stored secret
        if (token == null || token.isBlank()) {
            log.warn("Webhook rejected: missing X-Gitlab-Token header");
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing webhook token");
        }

        Long projectId = payload.getProjectId();
        if (projectId == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Missing project.id");
        }

        Optional<RepoEntity> repoOpt = repoService.findByGitlabProjectId(projectId);
        if (repoOpt.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Repo not found for project: " + projectId);
        }

        // Constant-time comparison of token vs stored (encrypted) webhook secret
        String storedSecret = repoOpt.get().getWebhookSecret();
        if (storedSecret != null && !storedSecret.isEmpty()) {
            String expectedSecret = cryptoService.decrypt(storedSecret);
            if (!constantTimeEquals(token, expectedSecret)) {
                log.warn("Webhook rejected: token mismatch for projectId={}", projectId);
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook token");
            }
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

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] ab = a.getBytes();
        byte[] bb = b.getBytes();
        if (ab.length != bb.length) return false;
        int diff = 0;
        for (int i =  0; i < ab.length; i++) {
            diff |= ab[i] ^ bb[i];
        }
        return diff == 0;
    }
}
