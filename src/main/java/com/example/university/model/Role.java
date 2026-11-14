package com.example.university.model;

public enum Role {
    ADMIN, TEACHER, STUDENT;

    public static Role fromResource(String resource) {
        return switch (resource) {
            case "admins" -> ADMIN;
            case "teachers" -> TEACHER;
            case "students" -> STUDENT;
            default -> throw new IllegalArgumentException("Unknown resource");
        };
    }
}

