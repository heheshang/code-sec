package com.codesec.api.e2e;

import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class E10RbacDenyE2ETest extends BaseE2ETest {

    @Test
    void shouldDenyUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/vulns"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldAllowAuthenticatedAccess() throws Exception {
        String token = getAdminToken();
        mockMvc.perform(get("/api/v1/vulns")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
