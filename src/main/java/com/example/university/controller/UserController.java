package com.example.university.controller;

import com.example.university.dto.*;
import com.example.university.model.Role;
import com.example.university.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/{resource:admins|teachers|students}")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public List<UserResponse> list(@PathVariable String resource) {
        return users.list(Role.fromResource(resource));
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable String resource, @PathVariable Long id) {
        return users.get(id, Role.fromResource(resource));
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@PathVariable String resource,
                                               @Valid @RequestBody CreateUserRequest request,
                                               Principal principal) {
        UserResponse user = users.create(Role.fromResource(resource), request, principal.getName());
        return ResponseEntity.created(URI.create("/api/" + resource + "/" + user.id())).body(user);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable String resource, @PathVariable Long id,
                               @Valid @RequestBody UpdateUserRequest request, Principal principal) {
        return users.update(id, Role.fromResource(resource), request, principal.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String resource, @PathVariable Long id,
                                       Principal principal) {
        users.delete(id, Role.fromResource(resource), principal.getName());
        return ResponseEntity.noContent().build();
    }
}

