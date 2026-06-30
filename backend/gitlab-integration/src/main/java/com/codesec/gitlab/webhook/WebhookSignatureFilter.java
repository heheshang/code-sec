package com.codesec.gitlab.webhook;

import com.codesec.gitlab.GitLabProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.Duration;

/**
 * Spring OncePerRequestFilter that validates the X-Gitlab-Token header
 * and performs UUID-based replay protection.
 *
 * <h3>Authentication</h3>
 * Stock GitLab sends the webhook secret as a plaintext value in the
 * {@code X-Gitlab-Token} header.  This filter compares it against the
 * locally-configured secret using constant-time comparison.
 *
 * <h3>Replay Protection</h3>
 * For GitLab ≥ 14.5, the {@code X-Gitlab-Event-UUID} header is used
 * with a local Caffeine cache (30s TTL) to detect duplicate events.
 * Redis {@code SETNX} will replace Caffeine when available (Sprint 3+).
 */
@Component
public class WebhookSignatureFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureFilter.class);

    private static final String HEADER_TOKEN = "X-Gitlab-Token";
    private static final String HEADER_EVENT_UUID = "X-Gitlab-Event-UUID";

    private final GitLabProperties properties;
    private final Cache<String, Boolean> eventUuidCache;

    public WebhookSignatureFilter(GitLabProperties properties) {
        this.properties = properties;
        this.eventUuidCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofSeconds(30))
            .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
        throws ServletException, IOException {

        // ── 1. Token presence check ────────────────────────────────
        String token = request.getHeader(HEADER_TOKEN);
        if (token == null || token.isEmpty()) {
            log.warn("Webhook rejected: missing {} header from {}", HEADER_TOKEN, request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                "Missing X-Gitlab-Token header");
            return;
        }

        // ── 2. Token comparison (constant-time) ────────────────────
        String secret = properties.webhookSecret();
        if (secret == null || secret.isEmpty()) {
            log.warn("Webhook rejected: no webhook secret configured");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                "Webhook secret not configured");
            return;
        }

        if (!constantTimeEquals(token, secret)) {
            log.warn("Webhook rejected: X-Gitlab-Token mismatch from {}", request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                "Invalid X-Gitlab-Token");
            return;
        }

        // ── 3. UUID replay protection (GitLab ≥ 14.5) ─────────────
        String eventUuid = request.getHeader(HEADER_EVENT_UUID);
        if (eventUuid != null && !eventUuid.isEmpty()) {
            Boolean existing = eventUuidCache.getIfPresent(eventUuid);
            if (existing != null) {
                log.info("Webhook UUID replay detected: {} (returning 200 without reprocessing)", eventUuid);
                // Return 200 so GitLab doesn't retry, but do NOT trigger scan
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":\"duplicate\",\"uuid\":\"" + eventUuid + "\"}");
                return;
            }
            eventUuidCache.put(eventUuid, true);
        }

        // ── 4. Pass through ────────────────────────────────────────
        filterChain.doFilter(request, response);
    }

    /** Constant-time string comparison using MessageDigest to prevent timing attacks. */
    static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                      b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Visible for testing. */
    Cache<String, Boolean> eventUuidCache() {
        return eventUuidCache;
    }
}
