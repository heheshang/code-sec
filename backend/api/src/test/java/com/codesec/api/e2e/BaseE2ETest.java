package com.codesec.api.e2e;

import com.codesec.api.ApiApplication;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

/**
 * Base class for all API E2E tests.
 *
 * Uses MOCK web environment (no embedded Tomcat) to avoid
 * SocketException: Operation not permitted in sandbox environments.
 * MockMvc provides full request/response simulation without a real server.
 *
 * All test methods access endpoints via mockMvc, not baseUrl().
 * No test subclasses currently use baseUrl() or @LocalServerPort.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = ApiApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class BaseE2ETest {

    @Autowired
    protected MockMvc mockMvc;

    protected String getAdminToken() throws Exception {
        var result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .post("/api/v1/auth/login")
            .contentType("application/json")
            .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
            .andReturn();
        String body = result.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(body);
        return node.get("token").asText();
    }
}
