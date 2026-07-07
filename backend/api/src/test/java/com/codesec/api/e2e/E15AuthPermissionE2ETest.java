package com.codesec.api.e2e;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E test for the AuthController dynamic permission fix (M1-S3 T1).
 *
 * Verifies:
 * 1. Admin login returns dynamically resolved permissions (not the old hardcoded 23-item list)
 * 2. The permissions list matches the seeded role-permission assignments (30 items)
 * 3. The role name is correctly resolved from the user_role table
 */
public class E15AuthPermissionE2ETest extends BaseE2ETest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_shouldReturnDynamicPermissions() throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);

        // Verify token is present
        assertNotNull(node.get("token"), "JWT token should be present");
        assertFalse(node.get("token").asText().isEmpty(), "JWT token should not be empty");

        // Verify user object
        JsonNode user = node.get("user");
        assertNotNull(user, "user object should be present");

        // Verify role is SUPER_ADMIN (not the fallback READONLY_VIEWER)
        String role = user.get("role").asText();
        assertEquals("SUPER_ADMIN", role,
            "Admin user should be assigned SUPER_ADMIN role, not fallback READONLY_VIEWER");

        // Verify permissions are dynamically loaded from DB (all perms for SUPER_ADMIN)
        JsonNode permissions = user.get("permissions");
        assertNotNull(permissions, "permissions list should be present");
        assertTrue(permissions.isArray(), "permissions should be an array");

        int permCount = 0;
        for (JsonNode perm : permissions) {
            assertFalse(perm.asText().isEmpty(), "permission should not be empty");
            permCount++;
        }

        // SUPER_ADMIN should have all permissions from seed data
        int expectedPermCount = 30;
        assertEquals(expectedPermCount, permCount,
            "SUPER_ADMIN should have " + expectedPermCount + " permissions (full set from seed data)");

        // Verify specific permissions are present
        List<String> permList = objectMapper.treeToValue(permissions, List.class);
        assertTrue(permList.contains("vuln:read"), "Should contain vuln:read");
        assertTrue(permList.contains("repo:create"), "Should contain repo:create");
        assertTrue(permList.contains("webhook:receive"), "Should contain webhook:receive");
        assertTrue(permList.contains("internal:vuln-index"), "Should contain internal:vuln-index");
    }

    @Test
    void login_shouldNotContainHardcodedFallbackPattern() throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        JsonNode user = node.get("user");

        // The old hardcoded implementation always set role to "SUPER_ADMIN" regardless of DB.
        // The new implementation resolves from DB. If role resolution fails or roles are empty,
        // the fallback is "READONLY_VIEWER". This should NOT happen for admin.
        assertNotEquals("READONLY_VIEWER", user.get("role").asText(),
            "Admin should not get fallback role — indicates dynamic resolution failed");
    }
}
