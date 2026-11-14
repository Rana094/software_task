package com.example.university.dto;

import jakarta.validation.constraints.*;

public record UpdateUserRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9_.-]{3,40}",
                message = "must be 3-40 letters, numbers, dots, underscores or hyphens") String username,
        @NotBlank @Email @Size(max = 128) String email,
        @NotBlank @Size(max = 80) String department,
        @Size(max = 72) String password) {
}

