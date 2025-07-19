package com.example.userservice.unit;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.util.UUID;

import com.example.userservice.dto.UserRequestDTO;  
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.entities.User;
import com.example.userservice.services.UserService;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.mappers.UserMapper;
import com.example.userservice.exceptions.ConflictException;
import com.example.userservice.exceptions.ResourceNotFoundException;

import static com.example.userservice.exceptions.ConflictException.USER_ALREADY_EXISTS;
import static com.example.userservice.exceptions.ResourceNotFoundException.USER_NOT_FOUND;


@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    UserRepository userRepository;

    @Mock
    UserMapper userMapper;

    @InjectMocks
    UserService userService;

    @Test
    void createUser_shouldSaveAndReturnUserDTO_whenEmailDoesNotExist() {
        UserRequestDTO userRequestDTO = UserRequestDTO.builder()
            .email("test@test.com")
            .build();

        User userEntity = User.builder()
            .email(userRequestDTO.getEmail())
            .build();

        UUID userId = UUID.randomUUID();
        User savedUserEntity = User.builder()
            .id(userId)
            .email(userRequestDTO.getEmail())
            .build();

        UserResponseDTO userResponseDTO = UserResponseDTO.builder()
            .id(userId)
            .email(userRequestDTO.getEmail())
            .build();

        when(userRepository.existsByEmail(userRequestDTO.getEmail())).thenReturn(false);
        when(userMapper.toEntity(userRequestDTO)).thenReturn(userEntity);
        when(userRepository.save(userEntity)).thenReturn(savedUserEntity);
        when(userMapper.toDTO(savedUserEntity)).thenReturn(userResponseDTO);

        UserResponseDTO result = userService.createUser(userRequestDTO);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals(userRequestDTO.getEmail(), result.getEmail());

        verify(userRepository).existsByEmail(userRequestDTO.getEmail());
        verify(userRepository).save(userEntity);
        verify(userMapper).toEntity(userRequestDTO);
        verify(userMapper).toDTO(savedUserEntity);
    }

    @Test
    void createUser_shouldThrowConflictException_whenEmailExists() {
        UserRequestDTO userRequestDTO = UserRequestDTO.builder()
            .email("test@test.com")
            .build();

        when(userRepository.existsByEmail(userRequestDTO.getEmail())).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            userService.createUser(userRequestDTO);
        });

        assertEquals(String.format(USER_ALREADY_EXISTS, userRequestDTO.getEmail()), exception.getMessage());
        
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toEntity(any());
        verify(userMapper, never()).toDTO(any());
    }

    @Test
    void getUserById_shouldReturnUserDTO_whenUserExists(){
        UUID userId = UUID.randomUUID();
        User userEntity = User.builder()
            .id(userId)
            .build();

        UserResponseDTO userResponseDTO = UserResponseDTO.builder()
            .id(userId)
            .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userMapper.toDTO(userEntity)).thenReturn(userResponseDTO);

        UserResponseDTO result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());

        verify(userRepository).findById(userId);
        verify(userMapper).toDTO(userEntity);
    }

    @Test
    void getUserById_shouldThrowResourceNotFoundException_whenUserDoesntExists(){
        UUID userId = UUID.randomUUID();
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById(userId);
        });

        assertEquals(USER_NOT_FOUND, exception.getMessage());

        verify(userRepository).findById(userId);
        verify(userMapper, never()).toDTO(any());
    }

    @Test
    void getUsersByIds_shouldReturnUserDTOs_whenUsersExist(){
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        List<UUID> userIds = Arrays.asList(userId1, userId2);
        User userEntity1 = User.builder()
            .id(userId1)
            .build();
        User userEntity2 = User.builder()
            .id(userId2)
            .build();

        List<User> userEntities = Arrays.asList(userEntity1, userEntity2);

        UserResponseDTO userResponseDTO1 = UserResponseDTO.builder()
            .id(userId1)
            .build();

        UserResponseDTO userResponseDTO2 = UserResponseDTO.builder()
            .id(userId2)
            .build();

        when(userRepository.findUsersByIds(userIds)).thenReturn(userEntities);
        when(userMapper.toDTO(userEntity1)).thenReturn(userResponseDTO1);
        when(userMapper.toDTO(userEntity2)).thenReturn(userResponseDTO2);

        List<UserResponseDTO> result = userService.getUsersByIds(userIds);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(userResponseDTO1, result.get(0));
        assertEquals(userResponseDTO2, result.get(1));

        verify(userRepository).findUsersByIds(userIds);
        verify(userMapper).toDTO(userEntity1);
        verify(userMapper).toDTO(userEntity2);
    }

    @Test
    void getUsersByIds_shouldReturnUserDTOs_whenNotAllUsersExist(){
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        List<UUID> userIds = Arrays.asList(userId1, userId2);
        User userEntity1 = User.builder()
            .id(userId1)
            .build();

        List<User> userEntities = Arrays.asList(userEntity1);

        UserResponseDTO userResponseDTO1 = UserResponseDTO.builder()
            .id(userId1)
            .build();

        when(userRepository.findUsersByIds(userIds)).thenReturn(userEntities);
        when(userMapper.toDTO(userEntity1)).thenReturn(userResponseDTO1);

        List<UserResponseDTO> result = userService.getUsersByIds(userIds);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userResponseDTO1, result.get(0));

        verify(userRepository).findUsersByIds(userIds);
        verify(userMapper).toDTO(userEntity1);
    }

    @Test
    void getUsersByIds_shouldReturnEmptyList_whenNoUsersExist(){
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        List<UUID> userIds = Arrays.asList(userId1, userId2);
        when(userRepository.findUsersByIds(userIds)).thenReturn(Collections.emptyList());

        List<UserResponseDTO> result = userService.getUsersByIds(userIds);

        assertNotNull(result);
        assertEquals(0, result.size());

        verify(userRepository).findUsersByIds(userIds);
        verify(userMapper, never()).toDTO(any(User.class));
    }

    @Test
    void getUserByEmail_shouldReturnUserDTO_whenUserExists(){
        String email = "test@test.com";
        UUID userId = UUID.randomUUID();
        User userEntity = User.builder()
            .id(userId)
            .email(email)
            .build();

        UserResponseDTO userResponseDTO = UserResponseDTO.builder()
            .id(userId)
            .email(email)
            .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(userEntity));
        when(userMapper.toDTO(userEntity)).thenReturn(userResponseDTO);

        UserResponseDTO result = userService.getUserByEmail(email);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals(email, result.getEmail());

        verify(userRepository).findByEmail(email);
        verify(userMapper).toDTO(userEntity);
    }

    @Test
    void getUserByEmail_shouldThrowResourceNotFoundException_whenUserDoesntExists(){
        String email = "test@test.com";
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserByEmail(email);
        });

        assertEquals(USER_NOT_FOUND, exception.getMessage());

        verify(userRepository).findByEmail(email);
        verify(userMapper, never()).toDTO(any());
    }

    @Test
    void updateUser_shouldReturnUpdatedUserDTO_whenUserExists(){
        UUID userId = UUID.randomUUID();
        UserRequestDTO userRequestDTO = UserRequestDTO.builder()
            .email("new@test.com")
            .name("newName")
            .surname("newSurname")
            .birthDate(LocalDate.of(2000, 12, 31))
            .build();

        User userEntity = User.builder()
            .id(userId)
            .email("old@test.com")
            .name("oldName")
            .surname("oldSurname")
            .birthDate(LocalDate.of(1990, 2, 2))
            .build();

        User updatedUserEntity = User.builder()
            .id(userId)
            .email(userRequestDTO.getEmail())
            .name(userRequestDTO.getName())
            .surname(userRequestDTO.getSurname())
            .birthDate(userRequestDTO.getBirthDate())
            .build();

        UserResponseDTO userResponseDTO = UserResponseDTO.builder()
            .id(userId)
            .email(userRequestDTO.getEmail())
            .name(userRequestDTO.getName())
            .surname(userRequestDTO.getSurname())
            .birthDate(userRequestDTO.getBirthDate())
            .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userRepository.existsByEmail(userRequestDTO.getEmail())).thenReturn(false);
        doNothing().when(userMapper).updateEntity(userRequestDTO, userEntity);
        when(userRepository.save(userEntity)).thenReturn(updatedUserEntity);
        when(userMapper.toDTO(updatedUserEntity)).thenReturn(userResponseDTO);

        UserResponseDTO result = userService.updateUser(userId, userRequestDTO);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals(userRequestDTO.getEmail(), result.getEmail());
        assertEquals(userRequestDTO.getName(), result.getName());
        assertEquals(userRequestDTO.getSurname(), result.getSurname());
        assertEquals(userRequestDTO.getBirthDate(), result.getBirthDate());

        verify(userRepository).findById(userId);
        verify(userRepository).existsByEmail(userRequestDTO.getEmail());
        verify(userMapper).updateEntity(userRequestDTO, userEntity);
        verify(userRepository).save(userEntity);
        verify(userMapper).toDTO(updatedUserEntity);
    }

    @Test
    void updateUser_shouldThrowResourceNotFoundException_whenUserDoesntExists(){
        UUID userId = UUID.randomUUID();
        UserRequestDTO userRequestDTO = UserRequestDTO.builder()
            .email("new@test.com")
            .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateUser(userId, userRequestDTO);
        });

        assertEquals(USER_NOT_FOUND, exception.getMessage());

        verify(userRepository).findById(userId);
        verify(userRepository, never()).existsByEmail(any());
        verify(userMapper, never()).updateEntity(any(), any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toDTO(any());
    }

    @Test
    void updateUser_shouldThrowConflictException_whenEmailAlreadyExists(){
        UUID userId = UUID.randomUUID();
        UserRequestDTO userRequestDTO = UserRequestDTO.builder()
            .email("new@test.com")
            .build();

        User userEntity = User.builder()
            .id(userId)
            .email("old@test.com")
            .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userRepository.existsByEmail(userRequestDTO.getEmail())).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            userService.updateUser(userId, userRequestDTO);
        });

        assertEquals(String.format(USER_ALREADY_EXISTS, userRequestDTO.getEmail()), exception.getMessage());

        verify(userRepository).findById(userId);
        verify(userRepository).existsByEmail(userRequestDTO.getEmail());
        verify(userMapper, never()).updateEntity(any(), any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toDTO(any());
    }

    @Test
    void deleteUser_shouldDeleteUser(){
        UUID userId = UUID.randomUUID();

        doNothing().when(userRepository).deleteById(userId);

        userService.deleteUser(userId);

        verify(userRepository).deleteById(userId);
    }

}
