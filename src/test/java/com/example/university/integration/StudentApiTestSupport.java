package com.example.university.integration;

import com.example.university.dto.LoginRequest;
import com.example.university.model.Role;
import com.example.university.model.UserAccount;
import com.example.university.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Shares the same test student and login setup across the three API tests.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class StudentApiTestSupport {
    static final String EMAIL = "student@stud.kuet.ac.bd";
    private static final String PASSWORD = "StudentPass123!";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired ObjectMapper json;

    @BeforeEach
    void setUpStudent() {
        users.deleteAll();
        var student = new UserAccount();
        student.setUsername("student");
        student.setName("Test Student");
        student.setEmail(EMAIL);
        student.setDepartment("Computer Science");
        student.setRole(Role.STUDENT);
        student.setPassword(passwords.encode(PASSWORD));
        users.save(student);
    }

    String login() throws Exception {
        String body = json.writeValueAsString(new LoginRequest(EMAIL, PASSWORD));
        var result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        String token = json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        return "Bearer " + token;
    }
}
