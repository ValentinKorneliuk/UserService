package com.example.userservice.controllers;

import java.util.List;
import java.util.UUID;

import com.example.userservice.dto.CreateUserRequestDTO;
import dev.kornliuk.jwtauthstarter.exceptions.ForbiddenAccessException;
import dev.kornliuk.jwtauthstarter.security.UserDetailsProvider;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.userservice.dto.UpdateUserRequestDTO;
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.services.UserService;

import static dev.kornliuk.jwtauthstarter.exceptions.ForbiddenAccessException.ACCESS_FORBIDDEN;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserDetailsProvider userDetailsProvider;

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody CreateUserRequestDTO updateUserRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(updateUserRequestDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable UUID id) {
        if (!isOwner(id)) {
            throw new ForbiddenAccessException(ACCESS_FORBIDDEN);
        }
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/ids")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getUsersByIds(@RequestParam List<UUID> ids) {
        return ResponseEntity.ok(userService.getUsersByIds(ids));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequestDTO updateUserRequestDTO) {
        if (!isOwner(id)) {
            throw new ForbiddenAccessException(ACCESS_FORBIDDEN);
        }
        return ResponseEntity.ok(userService.updateUser(id, updateUserRequestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isOwner(UUID userId) {
        return userId.equals(userDetailsProvider.getUserId());
    }
}