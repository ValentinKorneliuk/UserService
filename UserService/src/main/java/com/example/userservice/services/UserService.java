package com.example.userservice.services;

import com.example.userservice.dto.CreateUserRequestDto;
import com.example.userservice.dto.UpdateUserRequestDTO;
import dev.kornliuk.jwtauthstarter.exceptions.ForbiddenAccessException;

import static dev.kornliuk.jwtauthstarter.exceptions.ForbiddenAccessException.ACCESS_FORBIDDEN;

import dev.kornliuk.jwtauthstarter.security.UserDetailsProvider;
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

@Service
@CacheConfig(cacheNames = "users")
public class UserService {

    private UserRepository userRepository;
    private UserMapper userMapper;
    private UserDetailsProvider userDetailsProvider;

    public UserService(UserRepository userRepository, UserMapper userMapper, UserDetailsProvider userDetailsProvider) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.userDetailsProvider = userDetailsProvider;
    }

    public UserResponseDTO createUser(CreateUserRequestDto createUserRequestDTO) {
        if (userRepository.existsByEmail(createUserRequestDTO.getEmail())) {
            throw new ConflictException(
                    String.format(ConflictException.USER_ALREADY_EXISTS, createUserRequestDTO.getEmail())
            );
        }
        User user = userMapper.toEntity(createUserRequestDTO);
        User savedUser = userRepository.save(user);
        return userMapper.toDTO(savedUser);
    }

    @Cacheable(key = "#id")
    public UserResponseDTO getUserById(UUID id) {
        if (!isOwner(id)) {
            throw new ForbiddenAccessException(ACCESS_FORBIDDEN);
        }
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
        if (!isOwner(id)) {
            throw new ForbiddenAccessException(ACCESS_FORBIDDEN);
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceNotFoundException.USER_NOT_FOUND));
        if (!user.getEmail().equals(updateUserRequestDTO.getEmail()) && userRepository.existsByEmail(updateUserRequestDTO.getEmail())) {
            throw new ConflictException(
                    String.format(ConflictException.USER_ALREADY_EXISTS, updateUserRequestDTO.getEmail())
            );
        }
        userMapper.updateEntity(updateUserRequestDTO, user);
        User updatedUser = userRepository.save(user);
        return userMapper.toDTO(updatedUser);
    }

    @Transactional
    @CacheEvict(key = "#id")
    public void deleteUser(UUID id) {
        if (!canDeleteUser(id)) {
            throw new ForbiddenAccessException(ACCESS_FORBIDDEN);
        }

        userRepository.deleteById(id);
    }

    private boolean isOwner(UUID userId) {
        return userId.equals(userDetailsProvider.getUserId());
    }

    private boolean isOwner(String email) {
        return email.equals(userDetailsProvider.getEmail());
    }

    private boolean canDeleteUser(UUID userId) {
        boolean isOwner = userId.equals(userDetailsProvider.getUserId());
        boolean isAdmin = userDetailsProvider.hasRole("ADMIN");

        return isOwner || isAdmin;
    }
}