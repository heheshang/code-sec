package com.codesec.gitlab;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator that probes GitLab API availability.
 *
 * <p>In auto mode, this drives the real/mock fallback decision:
 * if the version probe fails, the health indicator reports DOWN.
 */
@Component
public class GitLabHealthIndicator implements HealthIndicator {

    private final GitLabClient gitLabClient;
    private final GitLabProperties properties;

    public GitLabHealthIndicator(GitLabClient gitLabClient, GitLabProperties properties) {
        this.gitLabClient = gitLabClient;
        this.properties = properties;
    }

    @Override
    public Health health() {
        if (properties.isMockMode()) {
            return Health.up()
                .withDetail("mode", "mock")
                .withDetail("gitlab_url", properties.baseUrl())
                .build();
        }

        boolean reachable = gitLabClient.probeVersion();
        if (reachable) {
            return Health.up()
                .withDetail("mode", properties.mode())
                .withDetail("gitlab_url", properties.baseUrl())
                .build();
        }

        if (properties.isAutoMode()) {
            logAutoFallback();
            return Health.down()
                .withDetail("mode", "auto→mock (fallback)")
                .withDetail("reason", "GitLab API version probe failed")
                .withDetail("gitlab_url", properties.baseUrl())
                .build();
        }

        return Health.down()
            .withDetail("mode", "real")
            .withDetail("reason", "GitLab API unreachable in real mode")
            .withDetail("gitlab_url", properties.baseUrl())
            .build();
    }

    private void logAutoFallback() {
        var log = org.slf4j.LoggerFactory.getLogger(GitLabHealthIndicator.class);
        log.info("GitLab auto mode: version probe failed, falling back to mock. URL: {}",
            properties.baseUrl());
    }
}
