package com.codesec.api.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * E-S3-RULE: Rule whitelist/project exemption E2E tests.
 * Tests rule CRUD, sync, and project exemption flow.
 */
public class E13RuleWhitelistE2ETest extends BaseE2ETest {
    private String token;
    private Long repoId;
    private Long ruleId;

    @BeforeEach
    void setUp() throws Exception {
        token = getAdminToken();
        // Create a repo for exemption tests
        MvcResult repoResult = mockMvc.perform(post("/api/v1/repos")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                    {
                      "name": "rule-test-repo",
                      "platform": "gitlab",
                      "url": "https://gitlab.example.com/rule-test",
                      "accessToken": "test-token",
                      "businessLine": "Test",
                      "gitlabProjectId": 9999
                    }
                    """))
                .andExpect(status().isCreated())
                .andReturn();
        String repoBody = repoResult.getResponse().getContentAsString();
        repoId = Long.parseLong(
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(repoBody).get("id").asText()
        );
    }

    @Test
    void shouldSyncRulesFromEngine() throws Exception {
        mockMvc.perform(post("/api/v1/rules/sync")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").isNumber());
    }

    @Test
    void shouldListRules() throws Exception {
        // First sync to populate rules
        mockMvc.perform(post("/api/v1/rules/sync")
                .header("Authorization", "Bearer " + token));

        MvcResult result = mockMvc.perform(get("/api/v1/rules")
                .header("Authorization", "Bearer " + token)
                .param("page", "1")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        var tree = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        assertTrue(tree.get("total").asInt() > 0);
    }

    @Test
    void shouldGetRuleDetail() throws Exception {
        // Sync first
        mockMvc.perform(post("/api/v1/rules/sync")
                .header("Authorization", "Bearer " + token));

        // Get first rule
        MvcResult listResult = mockMvc.perform(get("/api/v1/rules")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        var tree = new com.fasterxml.jackson.databind.ObjectMapper().readTree(
            listResult.getResponse().getContentAsString());
        if (tree.get("total").asInt() > 0) {
            ruleId = tree.get("items").get(0).get("id").asLong();

            mockMvc.perform(get("/api/v1/rules/" + ruleId)
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ruleId").isString());
        }
    }

    @Test
    void shouldDisableRule() throws Exception {
        // Sync first
        mockMvc.perform(post("/api/v1/rules/sync")
                .header("Authorization", "Bearer " + token));

        // Get first rule
        MvcResult listResult = mockMvc.perform(get("/api/v1/rules")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        var tree = new com.fasterxml.jackson.databind.ObjectMapper().readTree(
            listResult.getResponse().getContentAsString());
        if (tree.get("total").asInt() > 0) {
            ruleId = tree.get("items").get(0).get("id").asLong();

            mockMvc.perform(put("/api/v1/rules/" + ruleId)
                    .header("Authorization", "Bearer " + token)
                    .contentType("application/json")
                    .content("{\"enabled\": false}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(false));
        }
    }

    @Test
    void shouldAddAndRemoveExemption() throws Exception {
        // Sync rules first
        mockMvc.perform(post("/api/v1/rules/sync")
                .header("Authorization", "Bearer " + token));

        // Get first rule
        MvcResult listResult = mockMvc.perform(get("/api/v1/rules")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        var tree = new com.fasterxml.jackson.databind.ObjectMapper().readTree(
            listResult.getResponse().getContentAsString());
        if (tree.get("total").asInt() > 0 && repoId != null) {
            ruleId = tree.get("items").get(0).get("id").asLong();

            // Add exemption
            mockMvc.perform(post("/api/v1/projects/" + repoId + "/exemptions")
                    .header("Authorization", "Bearer " + token)
                    .contentType("application/json")
                    .content("{\"ruleId\": " + ruleId + ", \"reason\": \"Known false positive\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.ruleId").value(ruleId));

            // List exemptions
            mockMvc.perform(get("/api/v1/projects/" + repoId + "/exemptions")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].ruleId").value(ruleId));

            // Remove exemption
            mockMvc.perform(delete("/api/v1/projects/" + repoId + "/exemptions/" + ruleId)
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    void shouldFilterBySeverity() throws Exception {
        // Sync first
        mockMvc.perform(post("/api/v1/rules/sync")
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(get("/api/v1/rules")
                .header("Authorization", "Bearer " + token)
                .param("severity", "high"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }
}
