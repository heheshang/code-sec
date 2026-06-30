package com.codesec.gitlab;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gitlab")
public record GitLabProperties(
    String baseUrl,
    String privateToken,
    int connectTimeoutSeconds,
    int readTimeoutSeconds,
    String mode,
    String webhookSecret
) {
    public GitLabProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://gitlab.example.com";
        }
        if (connectTimeoutSeconds <= 0) {
            connectTimeoutSeconds = 10;
        }
        if (readTimeoutSeconds <= 0) {
            readTimeoutSeconds = 30;
        }
        if (mode == null || mode.isBlank()) {
            mode = "auto";
        }
        if (webhookSecret == null) {
            webhookSecret = "";
        }
    }

    /** Returns true when real GitLab integration is active. */
    public boolean isRealMode() {
        return "real".equalsIgnoreCase(mode);
    }

    /** Returns true when mock/fallback mode is active. */
    public boolean isMockMode() {
        return "mock".equalsIgnoreCase(mode);
    }

    /** Returns true when auto-detection is enabled. */
    public boolean isAutoMode() {
        return "auto".equalsIgnoreCase(mode);
    }
}
