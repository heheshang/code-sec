package com.codesec.api.e2e;

import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class E11GitlabWebhookE2ETest extends BaseE2ETest {

    /**
     * The API module's duplicate WebhookController was deleted (F-006).
     * Webhook handling is now exclusively in the gitlab-integration module
     * under the same path — this test asserts 404 in the api-only context.
     */
    @Test
    void webhookEndpointNoLongerInApiModule() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/gitlab")
                .header("X-Gitlab-Token", "test-secret")
                .contentType("application/json")
                .content("""
                    {
                      "object_kind": "merge_request",
                      "project": {"id": 1001, "name": "test-service"},
                      "object_attributes": {
                        "iid": 1,
                        "source_branch": "feature/test",
                        "target_branch": "main",
                        "last_commit": {"id": "abc123def456"}
                      }
                    }
                    """))
                .andExpect(status().isNotFound());
    }
}
