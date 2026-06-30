package com.codesec.gitlab.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Routes incoming GitLab webhook events based on {@code X-Gitlab-Event} header
 * and {@code object_kind} field in the JSON body.
 *
 * <p>v1 supports only Merge Request hooks; other event types are logged and skipped.
 */
@Component
public class WebhookEventRouter {

    private static final Logger log = LoggerFactory.getLogger(WebhookEventRouter.class);

    private final MergeRequestEventHandler mrEventHandler;

    public WebhookEventRouter(MergeRequestEventHandler mrEventHandler) {
        this.mrEventHandler = mrEventHandler;
    }

    /**
     * Routes the webhook payload to the appropriate handler.
     *
     * @param eventType the X-Gitlab-Event header value (e.g. "Merge Request Hook")
     * @param payload   the parsed JSON payload
     * @return routing result
     */
    public RouteResult route(String eventType, Map<String, Object> payload) {
        if (eventType == null) {
            log.warn("Webhook event received without X-Gitlab-Event header");
            return RouteResult.skipped("missing_event_type");
        }

        String objectKind = payload != null
            ? (String) payload.getOrDefault("object_kind", "")
            : "";

        // Merge Request Hook
        if ("Merge Request Hook".equalsIgnoreCase(eventType)
            || "merge_request".equalsIgnoreCase(objectKind)) {

            @SuppressWarnings("unchecked")
            Map<String, Object> objectAttributes =
                (Map<String, Object>) payload.get("object_attributes");

            String action = objectAttributes != null
                ? (String) objectAttributes.getOrDefault("action", "")
                : "";

            // Only process open/update/reopen actions
            if (!isProcessableAction(action)) {
                log.info("Webhook MR action '{}' skipped (not open/update/reopen)", action);
                return RouteResult.skipped("mr_action_" + action);
            }

            mrEventHandler.handle(payload);
            return RouteResult.processed("merge_request");
        }

        log.info("Webhook event type '{}' not supported in v1, skipping", eventType);
        return RouteResult.skipped("unsupported_event_" + eventType);
    }

    private boolean isProcessableAction(String action) {
        return "open".equals(action) || "update".equals(action) || "reopen".equals(action);
    }

    /** Result of webhook event routing. */
    public record RouteResult(String status, String detail) {
        public static RouteResult processed(String detail) {
            return new RouteResult("processed", detail);
        }
        public static RouteResult skipped(String detail) {
            return new RouteResult("skipped", detail);
        }
    }
}
