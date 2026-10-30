package com.app.warewise.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.app.warewise.entity.Users;
import com.app.warewise.repository.usersRepository;
import com.app.warewise.response.usersResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class usersController {

    private final usersRepository usersRepository;

    @GetMapping
    public ResponseEntity<List<usersResponse>> getAllUsers(@RequestParam(required = false) String role,
            @RequestParam(required = false) String name) {
        List<Users> users;

        if (role != null && !role.isBlank() && name != null && !name.isBlank()) {
            users = usersRepository.findByRoleContainingIgnoreCase(role.trim())
                    .stream()
                    .filter(user -> user.getName() != null && user.getName().toLowerCase().contains(name.trim().toLowerCase()))
                    .toList();
        } else if (role != null && !role.isBlank()) {
            users = usersRepository.findByRoleContainingIgnoreCase(role.trim());
        } else if (name != null && !name.isBlank()) {
            users = usersRepository.findByNameContainingIgnoreCase(name.trim());
        } else {
            users = usersRepository.findAll();
        }

        return ResponseEntity.ok(users.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<usersResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(findUser(id)));
    }

    @PostMapping
    public ResponseEntity<usersResponse> createUser(@RequestBody usersResponse request) {
        validateName(request.getName());
        validateRole(request.getRole());
        validateEmail(request.getEmail());

        Users user = new Users();
        user.setName(request.getName().trim());
        user.setRole(request.getRole().trim());
        user.setEmail(request.getEmail());
        user.setContact(request.getContact());

        Users saved = usersRepository.save(user);
        return ResponseEntity
                .created(URI.create("/api/users/" + saved.getId()))
                .body(toResponse(saved));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<usersResponse> patchUser(@PathVariable UUID id, @RequestBody usersResponse request) {
        Users user = findUser(id);

        if (request.getName() != null) {
            validateName(request.getName());
            user.setName(request.getName().trim());
        }
        if (request.getRole() != null) {
            validateRole(request.getRole());
            user.setRole(request.getRole().trim());
        }
        if (request.getEmail() != null) {
            validateEmail(request.getEmail());
            user.setEmail(request.getEmail());
        }
        if (request.getContact() != null) {
            user.setContact(request.getContact());
        }

        return ResponseEntity.ok(toResponse(usersRepository.save(user)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        Users user = findUser(id);
        try {
            usersRepository.delete(user);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User is linked to purchase orders or waste records and cannot be deleted");
        }
        return ResponseEntity.noContent().build();
    }

    private Users findUser(UUID id) {
        return usersRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User name is required");
        }
    }

    private void validateRole(String role) {
        if (role == null || role.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User role is required");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User email is required");
        }
    }

    private usersResponse toResponse(Users user) {
        return new usersResponse(
                user.getId(),
                user.getName(),
                user.getRole(),
                user.getEmail(),
                user.getContact(),
                user.getCreatedAt());
    }
}
