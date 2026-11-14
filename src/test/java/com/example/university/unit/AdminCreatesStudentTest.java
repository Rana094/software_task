package com.example.university.unit;

import com.example.university.dto.CreateUserRequest;
import com.example.university.model.Role;
import com.example.university.model.UserAccount;
import com.example.university.repository.UserRepository;
import com.example.university.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminCreatesStudentTest {
    @Test
    void savesStudentWithHashedPassword() {
        var users = mock(UserRepository.class);
        var passwords = new BCryptPasswordEncoder();
        var service = new UserService(users, passwords);
        var admin = new UserAccount();
        admin.setRole(Role.ADMIN);
        String adminEmail = "admin@example.com";
        when(users.findByEmailIgnoreCase(adminEmail)).thenReturn(Optional.of(admin));
        when(users.saveAndFlush(any(UserAccount.class))).thenAnswer(returnsFirstArg());
        var request = new CreateUserRequest("Test Student", "student", "student@stud.kuet.ac.bd",
                "Computer Science", "StudentPass123!");

        service.create(Role.STUDENT, request, adminEmail);

        // Inspect the account passed to the repository.
        var saved = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).saveAndFlush(saved.capture());
        assertEquals(Role.STUDENT, saved.getValue().getRole());
        String hash = saved.getValue().getPassword();
        assertNotEquals(request.password(), hash);
        assertTrue(passwords.matches(request.password(), hash));
    }
}
