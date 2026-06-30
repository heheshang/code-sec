package com.codesec.api.e2e;

import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class E9TicketStateFlowE2ETest extends BaseE2ETest {

    @Test
    void shouldListTickets() throws Exception {
        String token = getAdminToken();
        mockMvc.perform(get("/api/v1/tickets?page=1&size=20")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void shouldRejectIllegalTransition() throws Exception {
        String token = getAdminToken();
        // Try to transition a non-existent ticket to closed (should fail)
        mockMvc.perform(post("/api/v1/tickets/999999/transition")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"toStatus\": \"closed\"}"))
                .andExpect(status().is4xxClientError());
    }
}
