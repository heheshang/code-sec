package com.codesec.api.e2e;

import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class E4ScanStatusE2ETest extends BaseE2ETest {

    @Test
    void shouldListRepos() throws Exception {
        String token = getAdminToken();
        mockMvc.perform(get("/api/v1/repos")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }
}
