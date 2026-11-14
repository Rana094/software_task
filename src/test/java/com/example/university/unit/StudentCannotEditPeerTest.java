package com.example.university.unit;

import com.example.university.dto.UpdateUserRequest;
import com.example.university.model.Role;
import com.example.university.model.UserAccount;
import com.example.university.repository.UserRepository;
import com.example.university.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentCannotEditPeerTest {
    @Test
    void rejectsAnEditToAnotherStudentsAccount() {
        var users = mock(UserRepository.class);
        var service = new UserService(users, new BCryptPasswordEncoder());
        var student = new UserAccount();
        var peer = new UserAccount();
        student.setRole(Role.STUDENT);
        peer.setRole(Role.STUDENT);
        // The database normally assigns these IDs. The two students have different IDs.
        ReflectionTestUtils.setField(student, "id", 1L);
        ReflectionTestUtils.setField(peer, "id", 2L);
        String studentEmail = "student@stud.kuet.ac.bd";
        when(users.findByEmailIgnoreCase(studentEmail)).thenReturn(Optional.of(student));
        when(users.findByIdAndRole(2L, Role.STUDENT)).thenReturn(Optional.of(peer));
        var request = new UpdateUserRequest("Changed Name", "peer", "peer@stud.kuet.ac.bd",
                "Computer Science", "");

        var error = assertThrows(ResponseStatusException.class,
                () -> service.update(2L, Role.STUDENT, request, studentEmail));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(users, never()).saveAndFlush(any(UserAccount.class));
    }
}
