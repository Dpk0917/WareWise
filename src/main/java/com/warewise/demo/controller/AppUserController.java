package com.warewise.demo.controller;

import com.warewise.demo.dto.AppUserDto;
import com.warewise.demo.services.AppUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService userService;

    // CREATE
    @PostMapping
    public ResponseEntity<AppUserDto> createUser(
            @Valid @RequestBody AppUserDto dto
    ) {

        AppUserDto user =
                userService.createUser(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(user);
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<AppUserDto> getUserById(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                userService.getUserById(id)
        );
    }

    // GET ALL
    @GetMapping
    public ResponseEntity<List<AppUserDto>> getAllUsers() {

        return ResponseEntity.ok(
                userService.getAllUsers()
        );
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<AppUserDto> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody AppUserDto dto
    ) {

        return ResponseEntity.ok(
                userService.updateUser(id, dto)
        );
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID id
    ) {

        userService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }
}