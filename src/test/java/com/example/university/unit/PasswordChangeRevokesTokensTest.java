package com.example.university.unit;

import com.example.university.dto.UpdateUserRequest;
import com.example.university.model.Role;
import com.example.university.model.UserAccount;
import com.example.university.repository.UserRepository;
import com.example.university.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordChangeRevokesTokensTest {
    @Test
    void hashesTheNewPasswordAndInvalidatesTheOldTokens() {
        var users = mock(UserRepository.class);
        var passwords = new BCryptPasswordEncoder();
        var service = new UserService(users, passwords);
        String email = "student@stud.kuet.ac.bd";
        String oldPassword = "StudentPass123!";
        String newPassword = "NewStudentPass123!";
        var student = new UserAccount();
        ReflectionTestUtils.setField(student, "id", 1L);
        student.setEmail(email);
        student.setRole(Role.STUDENT);
        student.setPassword(passwords.encode(oldPassword));
        when(users.findByEmailIgnoreCase(email)).thenReturn(Optional.of(student));
        when(users.findByIdAndRole(1L, Role.STUDENT)).thenReturn(Optional.of(student));
        when(users.saveAndFlush(student)).thenReturn(student);
        var request = new UpdateUserRequest("Test Student", "student", email, "Computer Science", newPassword);

        service.update(1L, Role.STUDENT, request, email);

        assertTrue(passwords.matches(newPassword, student.getPassword()));
        assertFalse(passwords.matches(oldPassword, student.getPassword()));
        // A changed password should invalidate existing logins for this account.
        verify(users).invalidateTokens(1L);
    }
}
