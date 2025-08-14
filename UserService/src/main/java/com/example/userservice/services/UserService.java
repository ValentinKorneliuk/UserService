package com.example.userservice.services;

import com.example.userservice.dto.CreateUserRequestDTO;
import com.example.userservice.dto.UpdateUserRequestDTO;
import dev.kornliuk.jwtauthstarter.exceptions.ForbiddenAccessException;

import static dev.kornliuk.jwtauthstarter.exceptions.ForbiddenAccessException.ACCESS_FORBIDDEN;

import dev.kornliuk.jwtauthstarter.security.UserDetailsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.entities.User;
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.mappers.UserMapper;

import java.util.List;
import java.util.UUID;

import com.example.userservice.exceptions.ConflictException;
import com.example.userservice.exceptions.ResourceNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.CacheConfig;

@Slf4j
@Service
@CacheConfig(cacheNames = "users")
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserDetailsProvider userDetailsProvider;


    public UserResponseDTO createUser(CreateUserRequestDTO createUserRequestDTO) {
        if (userRepository.existsByEmail(createUserRequestDTO.getEmail())) {
            throw new ConflictException(
                    String.format(ConflictException.USER_ALREADY_EXISTS, createUserRequestDTO.getEmail())
            );
        }
        User user = userMapper.toEntity(createUserRequestDTO);
        User savedUser = userRepository.save(user);

        log.info("Created new user with id {}", user.getId());
        return userMapper.toDTO(savedUser);
    }

    @Cacheable(key = "#id")
    public UserResponseDTO getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceNotFoundException.USER_NOT_FOUND));

        return userMapper.toDTO(user);
    }

    public List<UserResponseDTO> getUsersByIds(List<UUID> ids) {
        List<User> users = userRepository.findUsersByIds(ids);
        return users.stream()
                .map(userMapper::toDTO)
                .toList();
    }

    @Cacheable(key = "#email")
    public UserResponseDTO getUserByEmail(String email) {
        if (!isOwner(email)) {
            throw new ForbiddenAccessException(ACCESS_FORBIDDEN);
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceNotFoundException.USER_NOT_FOUND));

        return userMapper.toDTO(user);
    }

    @Transactional
    @CachePut(key = "#id")
    public UserResponseDTO updateUser(UUID id, UpdateUserRequestDTO updateUserRequestDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceNotFoundException.USER_NOT_FOUND));
        if (!user.getEmail().equals(updateUserRequestDTO.getEmail()) && userRepository.existsByEmail(updateUserRequestDTO.getEmail())) {
            throw new ConflictException(
                    String.format(ConflictException.USER_ALREADY_EXISTS, updateUserRequestDTO.getEmail())
            );
        }
        userMapper.updateEntity(updateUserRequestDTO, user);
        User updatedUser = userRepository.save(user);

        log.info("Updated user with id {}", id);
        return userMapper.toDTO(updatedUser);
    }

    @Transactional
    @CacheEvict(key = "#id")
    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
        log.info("Deleted user with id {}", id);
    }

    private boolean isOwner(String email) {
        return email.equals(userDetailsProvider.getEmail());
    }
}