package com.example.university.service;

import com.example.university.dto.*;
import com.example.university.model.Role;
import com.example.university.model.UserAccount;
import com.example.university.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder passwords;

    public UserService(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list(Role role) {
        return users.findByRoleOrderByIdAsc(role).stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id, Role role) {
        return UserResponse.from(find(id, role));
    }

    public UserResponse create(Role role, CreateUserRequest request, String currentEmail) {
        if (currentUser(currentEmail).getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can create accounts");
        }
        String username = normalize(request.username());
        String email = normalize(request.email());
        validateEmailDomain(role, email);
        if (users.existsByUsername(username) || users.existsByEmail(email)) {
            throw conflict("Username or email is already in use");
        }
        validatePassword(request.password());
        UserAccount user = new UserAccount();
        user.setName(request.name().trim());
        user.setUsername(username);
        user.setEmail(email);
        user.setDepartment(request.department().trim());
        user.setPassword(passwords.encode(request.password()));
        user.setRole(role); // The endpoint determines the role; clients cannot choose it.
        return UserResponse.from(users.saveAndFlush(user));
    }

    public UserResponse update(Long id, Role role, UpdateUserRequest request, String currentEmail) {
        UserAccount user = find(id, role);
        requireManageAccess(user, currentEmail);
        String username = normalize(request.username());
        String email = normalize(request.email());
        validateEmailDomain(role, email);
        if (users.existsByUsernameAndIdNot(username, id) || users.existsByEmailAndIdNot(email, id)) {
            throw conflict("Username or email is already in use");
        }
        String oldEmail = user.getEmail();
        boolean passwordChanged = request.password() != null && !request.password().isEmpty();
        if (passwordChanged) {
            validatePassword(request.password());
            user.setPassword(passwords.encode(request.password()));
        }
        user.setName(request.name().trim());
        user.setUsername(username);
        user.setEmail(email);
        user.setDepartment(request.department().trim());
        UserResponse result = UserResponse.from(users.saveAndFlush(user));
        if (passwordChanged || !oldEmail.equals(email)) {
            users.invalidateTokens(id);
        }
        return result;
    }

    public void delete(Long id, Role role, String currentEmail) {
        UserAccount user = find(id, role);
        requireManageAccess(user, currentEmail);
        if (user.getRole() == Role.ADMIN && user.getEmail().equalsIgnoreCase(currentEmail)) {
            throw conflict("Admins cannot delete their own account while logged in");
        }
        users.delete(user);
        users.flush();
    }

    private UserAccount currentUser(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in again"));
    }

    private void requireManageAccess(UserAccount target, String currentEmail) {
        UserAccount actor = currentUser(currentEmail);
        if (actor.getRole() != Role.ADMIN && !actor.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can edit or delete only your own account");
        }
    }

    private UserAccount find(Long id, Role role) {
        return users.findByIdAndRole(id, role)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found"));
    }

    private void validateEmailDomain(Role role, String email) {
        String domain = switch (role) {
            case STUDENT -> "stud.kuet.ac.bd";
            case TEACHER -> "cse.kuet.ac.bd";
            case ADMIN -> null;
        };
        if (domain != null && !email.endsWith("@" + domain)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    (role == Role.STUDENT ? "Student" : "Teacher") + " emails must use @" + domain);
        }
    }

    private void validatePassword(String password) {
        if (password.isBlank() || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 8 characters and at most 72 UTF-8 bytes");
        }
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}

