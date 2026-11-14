package com.example.university.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentLogoutTest extends StudentApiTestSupport {
    @Test
    void rejectsTheOldTokenAfterLogout() throws Exception {
        String token = login();

        mvc.perform(post("/api/auth/logout").header("Authorization", token))
                .andExpect(status().isNoContent());

        // Try the same token again after logging out.
        mvc.perform(get("/api/me").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }
}
