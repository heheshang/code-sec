package com.codesec.api.e2e;

import com.codesec.domain.entity.VulnFindingEntity;
import com.codesec.domain.repository.VulnFindingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class E5VulnListPaginationE2ETest extends BaseE2ETest {

    @Autowired
    private VulnFindingRepository vulnRepo;

    @Test
    void shouldListVulns() throws Exception {
        String token = getAdminToken();
        mockMvc.perform(get("/api/v1/vulns?page=1&size=20")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").isNumber());
    }
}
