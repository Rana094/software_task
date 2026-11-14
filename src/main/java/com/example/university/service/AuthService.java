package com.example.university.service;

import com.example.university.dto.LoginRequest;
import com.example.university.dto.TokenResponse;
import com.example.university.repository.UserRepository;
import com.example.university.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {
    private final AuthenticationManager authentication;
    private final UserRepository users;
    private final JwtService jwt;

    public AuthService(AuthenticationManager authentication, UserRepository users, JwtService jwt) {
        this.authentication = authentication;
        this.users = users;
        this.jwt = jwt;
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalidCredentials();
        }
        try {
            var result = authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                    request.email().trim().toLowerCase(Locale.ROOT), request.password()));
            var user = users.findByEmailIgnoreCase(result.getName()).orElseThrow(this::invalidCredentials);
            return jwt.issue(user);
        } catch (AuthenticationException exception) {
            throw invalidCredentials();
        }
    }

    @Transactional
    public void logout(String email) {
        var user = users.findByEmailIgnoreCase(email).orElseThrow(this::invalidCredentials);
        // Logout revokes every outstanding token for this account, across tabs/devices.
        users.invalidateTokens(user.getId());
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}
