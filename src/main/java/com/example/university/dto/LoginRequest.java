package com.example.university.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank @Email @Size(max = 128) String email,
                           @NotBlank @Size(max = 72) String password) {
    public LoginRequest {
        if (email != null) email = email.trim();
    }
}
