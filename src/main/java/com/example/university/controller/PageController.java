package com.example.university.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // Public HTML shell. Its JavaScript loads all account data through JWT-protected APIs.
    @GetMapping("/")
    public String dashboard() {
        return "dashboard";
    }
}
