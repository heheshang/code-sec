package com.codesec.api.e2e;

import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class E3TriggerScanE2ETest extends BaseE2ETest {

    @Test
    void shouldCreateScanTask() throws Exception {
        String token = getAdminToken();
        // First create a repo
        mockMvc.perform(post("/api/v1/repos")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                    {"name":"scan-test","platform":"gitlab","url":"https://gitlab.example.com/test/scan","accessToken":"tok","gitlabProjectId":2001}
                    """))
                .andExpect(status().isCreated());

        // Then create scan task
        mockMvc.perform(post("/api/v1/scans")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                    {"repoId": 1, "mode": "full", "branch": "main"}
                    """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("queued"));
    }
}
