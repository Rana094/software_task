package com.example.university.dto;

import com.example.university.model.Role;
import com.example.university.model.UserAccount;

// Return this DTO instead of the entity so passwords never appear in API responses.
public record UserResponse(Long id, String name, String username, String email,
                           String department, Role role) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.getId(), user.getName(), user.getUsername(),
                user.getEmail(), user.getDepartment(), user.getRole());
    }
}

