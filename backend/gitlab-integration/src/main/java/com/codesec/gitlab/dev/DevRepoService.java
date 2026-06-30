package com.codesec.gitlab.dev;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Development/stub implementation of RepoService.
 *
 * <p><b>TEMPORARY:</b> In-memory repo registry for development. Replace
 * with real RepoService backed by a database when E-S2-CRITICAL delivers
 * the production implementation.
 *
 * @deprecated Replace with real RepoService per E-S2-CRITICAL.
 *     Scheduled removal: M2 engine integration sprint.
 */
@Deprecated(forRemoval = true)
@Component
public class DevRepoService {

    private final Map<Long, DevRepo> reposByGitlabProjectId = new ConcurrentHashMap<>();

    public DevRepoService() {
        registerRepo(1L, "test-project", "glpat-test-token-12345");
    }

    public void registerRepo(long gitlabProjectId, String name, String webhookSecret) {
        DevRepo repo = new DevRepo(gitlabProjectId, name, webhookSecret);
        reposByGitlabProjectId.put(gitlabProjectId, repo);
    }

    public Optional<DevRepo> findByGitlabProjectId(long gitlabProjectId) {
        return Optional.ofNullable(reposByGitlabProjectId.get(gitlabProjectId));
    }

    public Optional<String> getWebhookSecret(long gitlabProjectId) {
        return findByGitlabProjectId(gitlabProjectId)
            .map(DevRepo::webhookSecret);
    }

    public record DevRepo(long gitlabProjectId, String name, String webhookSecret) {}
}
