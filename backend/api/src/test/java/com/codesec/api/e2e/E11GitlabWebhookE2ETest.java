package com.codesec.api.e2e;

import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class E11GitlabWebhookE2ETest extends BaseE2ETest {

    @Test
    void shouldAcceptGitlabWebhook() throws Exception {
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));
    }
}
