package com.warewise.demo.services;

import com.warewise.demo.dto.AppUserDto;
import com.warewise.demo.entity.AppUser;
import com.warewise.demo.repository.AppUserRepository;
import com.warewise.demo.exception.DuplicateResourceException;
import com.warewise.demo.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AppUserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // CREATE
    public AppUserDto createUser(AppUserDto dto) {

        String email = dto.getEmail()
                .trim()
                .toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException(
                    "User with this email already exists"
            );
        }

        AppUser user = AppUser.builder()
                .name(dto.getName().trim())
                .email(email)
                .passwordHash(
                        passwordEncoder.encode(dto.getPassword())
                )
                .role(dto.getRole())
                .build();

        AppUser savedUser = userRepository.save(user);

        return mapToDto(savedUser);
    }

    // GET BY ID
    @Transactional(readOnly = true)
    public AppUserDto getUserById(UUID id) {

        AppUser user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + id
                        )
                );

        return mapToDto(user);
    }

    // GET ALL
    @Transactional(readOnly = true)
    public List<AppUserDto> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    // UPDATE
    public AppUserDto updateUser(
            UUID id,
            AppUserDto dto
    ) {

        AppUser user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + id
                        )
                );

        if (dto.getName() != null
                && !dto.getName().isBlank()) {

            user.updateName(
                    dto.getName().trim()
            );
        }

        if (dto.getEmail() != null
                && !dto.getEmail().isBlank()) {

            String email = dto.getEmail()
                    .trim()
                    .toLowerCase();

            if (!email.equalsIgnoreCase(user.getEmail())
                    && userRepository.existsByEmailIgnoreCase(email)) {

                throw new DuplicateResourceException(
                        "Email is already being used"
                );
            }

            user.updateEmail(email);
        }

        if (dto.getRole() != null) {
            user.changeRole(dto.getRole());
        }

        if (dto.getPassword() != null
                && !dto.getPassword().isBlank()) {

            user.updatePassword(
                    passwordEncoder.encode(dto.getPassword())
            );
        }

        return mapToDto(user);
    }

    // DELETE
    public void deleteUser(UUID id) {

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "User not found with id: " + id
            );
        }

        userRepository.deleteById(id);
    }

    // ENTITY -> DTO
    private AppUserDto mapToDto(AppUser user) {

        return AppUserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}