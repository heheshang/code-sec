package com.codesec.api.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

public class E1CreateRepoE2ETest extends BaseE2ETest {

    @Test
    void shouldCreateRepoAndEncryptToken() throws Exception {
        String token = getAdminToken();
        MvcResult result = mockMvc.perform(post("/api/v1/repos")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                    {
                      "name": "test-service",
                      "platform": "gitlab",
                      "url": "https://gitlab.example.com/test/service",
                      "accessToken": "glpat-secret-token",
                      "businessLine": "Platform",
                      "gitlabProjectId": 1001
                    }
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("test-service"))
                .andExpect(jsonPath("$.hasToken").value(true))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("test-service"));
        assertFalse(body.contains("glpat-secret-token"));
    }
}
