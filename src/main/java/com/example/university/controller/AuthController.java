package com.example.university.controller;

import com.example.university.dto.LoginRequest;
import com.example.university.dto.TokenResponse;
import com.example.university.dto.UserResponse;
import com.example.university.repository.UserRepository;
import com.example.university.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;

@RestController
public class AuthController {
    private final UserRepository users;
    private final AuthService auth;

    public AuthController(UserRepository users, AuthService auth) {
        this.users = users;
        this.auth = auth;
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(auth.login(request));
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(Principal principal) {
        auth.logout(principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/me")
    public UserResponse me(Principal principal) {
        return users.findByEmailIgnoreCase(principal.getName()).map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in again"));
    }
}
