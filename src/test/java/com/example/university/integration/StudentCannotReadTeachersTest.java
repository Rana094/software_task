package com.example.university.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentCannotReadTeachersTest extends StudentApiTestSupport {
    @Test
    void rejectsATeacherRequestFromAStudent() throws Exception {
        String token = login();

        // The student is signed in but lacks permission to read teacher data.
        mvc.perform(get("/api/teachers").header("Authorization", token))
                .andExpect(status().isForbidden());
    }
}
