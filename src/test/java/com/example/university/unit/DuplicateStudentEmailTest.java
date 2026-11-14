package com.example.university.unit;

import com.example.university.dto.CreateUserRequest;
import com.example.university.model.Role;
import com.example.university.model.UserAccount;
import com.example.university.repository.UserRepository;
import com.example.university.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DuplicateStudentEmailTest {
    @Test
    void rejectsAnEmailThatAlreadyExists() {
        var users = mock(UserRepository.class);
        var service = new UserService(users, new BCryptPasswordEncoder());
        var admin = new UserAccount();
        admin.setRole(Role.ADMIN);
        String adminEmail = "admin@example.com";
        String existingEmail = "taken@stud.kuet.ac.bd";
        when(users.findByEmailIgnoreCase(adminEmail)).thenReturn(Optional.of(admin));
        // Simulate a repository that already has this email.
        when(users.existsByEmail(existingEmail)).thenReturn(true);
        var request = new CreateUserRequest("New Student", "newstudent", existingEmail,
                "Computer Science", "StudentPass123!");

        var error = assertThrows(ResponseStatusException.class,
                () -> service.create(Role.STUDENT, request, adminEmail));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(users, never()).saveAndFlush(any(UserAccount.class));
    }
}
