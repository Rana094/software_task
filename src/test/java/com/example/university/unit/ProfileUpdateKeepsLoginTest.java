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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileUpdateKeepsLoginTest {
    @Test
    void keepsPasswordAndTokensWhenOnlyTheNameChanges() {
        var users = mock(UserRepository.class);
        var passwords = new BCryptPasswordEncoder();
        var service = new UserService(users, passwords);
        String email = "student@stud.kuet.ac.bd";
        String originalHash = passwords.encode("StudentPass123!");
        var student = new UserAccount();
        ReflectionTestUtils.setField(student, "id", 1L);
        student.setName("Old Name");
        student.setUsername("student");
        student.setEmail(email);
        student.setDepartment("Computer Science");
        student.setRole(Role.STUDENT);
        student.setPassword(originalHash);
        when(users.findByEmailIgnoreCase(email)).thenReturn(Optional.of(student));
        when(users.findByIdAndRole(1L, Role.STUDENT)).thenReturn(Optional.of(student));
        when(users.saveAndFlush(student)).thenReturn(student);
        // An empty password tells the service to keep the existing password.
        var request = new UpdateUserRequest("New Name", "student", email, "Computer Science", "");

        var response = service.update(1L, Role.STUDENT, request, email);

        assertEquals("New Name", response.name());
        assertEquals(originalHash, student.getPassword());
        verify(users, never()).invalidateTokens(1L);
    }
}
