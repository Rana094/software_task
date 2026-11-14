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

class StudentCannotCreateAccountTest {
    @Test
    void rejectsCreationByStudent() {
        var users = mock(UserRepository.class);
        var service = new UserService(users, new BCryptPasswordEncoder());
        var student = new UserAccount();
        student.setRole(Role.STUDENT);
        String studentEmail = "student@stud.kuet.ac.bd";
        when(users.findByEmailIgnoreCase(studentEmail)).thenReturn(Optional.of(student));
        var request = new CreateUserRequest("New Student", "newstudent", "new@stud.kuet.ac.bd",
                "Computer Science", "StudentPass123!");

        // Use valid input so the only reason to reject it is the caller's role.
        var error = assertThrows(ResponseStatusException.class,
                () -> service.create(Role.STUDENT, request, studentEmail));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(users, never()).saveAndFlush(any(UserAccount.class));
    }
}
