package com.codesec.gitlab.webhook;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * GitLab Webhook receiver endpoint.
 *
 * <p>Receives POST requests at {@code /api/v1/webhooks/gitlab} (as locked in
 * E-S2-CRITICAL § 3.5.1).  Signature validation is handled by
 * {@link WebhookSignatureFilter} registered in the filter chain.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookEventRouter eventRouter;
    private final ObjectMapper objectMapper;

    public WebhookController(WebhookEventRouter eventRouter, ObjectMapper objectMapper) {
        this.eventRouter = eventRouter;
        this.objectMapper = objectMapper;
    }

    /**
     * Receives GitLab webhook events.
     *
     * <p>Signature validation is performed by {@link WebhookSignatureFilter}
     * before this method is invoked.
     */
    @PostMapping("/gitlab")
    public ResponseEntity<Map<String, String>> receiveWebhook(
        @RequestBody String rawBody,
        @RequestHeader(value = "X-Gitlab-Event", required = false) String eventType,
        HttpServletRequest request) {

        log.info("Webhook received: event={}, contentType={}, contentLength={}",
            eventType,
            request.getContentType(),
            request.getContentLengthLong());

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(rawBody,
                new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse webhook JSON body: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", "Invalid JSON payload"));
        }

        WebhookEventRouter.RouteResult result = eventRouter.route(eventType, payload);

        return ResponseEntity.ok(Map.of(
            "status", result.status(),
            "detail", result.detail()
        ));
    }
}
