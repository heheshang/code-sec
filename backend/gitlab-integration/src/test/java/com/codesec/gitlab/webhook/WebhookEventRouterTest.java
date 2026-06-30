package com.codesec.gitlab.webhook;

import com.codesec.gitlab.scan.MrScanOrchestrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebhookEventRouter")
class WebhookEventRouterTest {

    /** Test stub that counts handle() calls instead of using Mockito. */
    private static class CountingEventHandler extends MergeRequestEventHandler {
        final AtomicInteger callCount = new AtomicInteger(0);

        CountingEventHandler() {
            super(null); // null orchestrator – unused in routing tests
        }

        @Override
        public void handle(Map<String, Object> payload) {
            callCount.incrementAndGet();
        }
    }

    @Nested
    @DisplayName("merge request routing")
    class MergeRequestRouting {

        @Test
        @DisplayName("should route open MR to handler")
        void routeOpenMr() {
            CountingEventHandler handler = new CountingEventHandler();
            WebhookEventRouter router = new WebhookEventRouter(handler);

            Map<String, Object> payload = Map.of(
                "object_kind", "merge_request",
                "object_attributes", Map.of("action", "open", "iid", 1)
            );

            WebhookEventRouter.RouteResult result = router.route("Merge Request Hook", payload);

            assertThat(result.status()).isEqualTo("processed");
            assertThat(handler.callCount.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("should route update MR action")
        void routeUpdateMr() {
            CountingEventHandler handler = new CountingEventHandler();
            WebhookEventRouter router = new WebhookEventRouter(handler);

            Map<String, Object> payload = Map.of(
                "object_kind", "merge_request",
                "object_attributes", Map.of("action", "update", "iid", 1)
            );

            router.route("Merge Request Hook", payload);
            assertThat(handler.callCount.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("should skip close action")
        void skipCloseMr() {
            CountingEventHandler handler = new CountingEventHandler();
            WebhookEventRouter router = new WebhookEventRouter(handler);

            Map<String, Object> payload = Map.of(
                "object_kind", "merge_request",
                "object_attributes", Map.of("action", "close", "iid", 1)
            );

            WebhookEventRouter.RouteResult result = router.route("Merge Request Hook", payload);

            assertThat(result.status()).isEqualTo("skipped");
            assertThat(handler.callCount.get()).isEqualTo(0);
        }

        @Test
        @DisplayName("should skip merge action")
        void skipMergeMr() {
            CountingEventHandler handler = new CountingEventHandler();
            WebhookEventRouter router = new WebhookEventRouter(handler);

            Map<String, Object> payload = Map.of(
                "object_kind", "merge_request",
                "object_attributes", Map.of("action", "merge", "iid", 1)
            );

            WebhookEventRouter.RouteResult result = router.route("Merge Request Hook", payload);

            assertThat(result.status()).isEqualTo("skipped");
            assertThat(handler.callCount.get()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("unsupported events")
    class UnsupportedEvents {

        @Test
        @DisplayName("should skip push events")
        void skipPushEvent() {
            CountingEventHandler handler = new CountingEventHandler();
            WebhookEventRouter router = new WebhookEventRouter(handler);
            Map<String, Object> payload = Map.of("object_kind", "push");

            WebhookEventRouter.RouteResult result = router.route("Push Hook", payload);

            assertThat(result.status()).isEqualTo("skipped");
            assertThat(handler.callCount.get()).isEqualTo(0);
        }
    }
}
