package com.example.userservice.unit;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import com.example.userservice.dto.UserRequestDTO;  
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.entities.User;
import com.example.userservice.services.UserService;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.mappers.UserMapper;
import com.example.userservice.exceptions.ConflictException;
import com.example.userservice.exceptions.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    
    @Mock
    UserRepository userRepository;

    @Mock
    UserMapper userMapper;

    @InjectMocks
    UserService userService;

    @Test
    public void createUser_shouldSaveAndReturnUserDTO_whenEmailDoesNotExist() {
        UserRequestDTO userRequestDTO = new UserRequestDTO();
        userRequestDTO.setEmail("test@test.com");

        User userEntity = new User();
        userEntity.setEmail(userRequestDTO.getEmail());

        User savedUserEntity = new User();
        savedUserEntity.setId(1L);
        savedUserEntity.setEmail(userRequestDTO.getEmail());

        UserResponseDTO userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(1L);
        userResponseDTO.setEmail(userRequestDTO.getEmail());

        when(userRepository.existsByEmail(userRequestDTO.getEmail())).thenReturn(false);
        when(userMapper.toEntity(userRequestDTO)).thenReturn(userEntity);
        when(userRepository.save(userEntity)).thenReturn(savedUserEntity);
        when(userMapper.toDTO(savedUserEntity)).thenReturn(userResponseDTO);

        UserResponseDTO result = userService.createUser(userRequestDTO);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(userRequestDTO.getEmail(), result.getEmail());

        verify(userRepository).existsByEmail(userRequestDTO.getEmail());
        verify(userRepository).save(userEntity);
        verify(userMapper).toEntity(userRequestDTO);
        verify(userMapper).toDTO(savedUserEntity);
    }

    @Test
    public void createUser_shouldThrowConflictException_whenEmailExists() {
        UserRequestDTO userRequestDTO = new UserRequestDTO();
        userRequestDTO.setEmail("test@test.com");

        when(userRepository.existsByEmail(userRequestDTO.getEmail())).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            userService.createUser(userRequestDTO);
        });

        assertEquals("User with email test@test.com already exists", exception.getMessage());
        
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toEntity(any());
        verify(userMapper, never()).toDTO(any());
    }

    @Test
    public void getUserById_shouldReturnUserDTO_whenUserExists(){
        Long userId = 1L;
        User userEntity = new User();

        UserResponseDTO userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userMapper.toDTO(userEntity)).thenReturn(userResponseDTO);

        UserResponseDTO result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());

        verify(userRepository).findById(userId);
        verify(userMapper).toDTO(userEntity);
    }

    @Test
    public void getUserById_shouldThrowResourceNotFoundException_whenUserDoesntExists(){
        Long userId = 1L;
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById(userId);
        });

        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findById(userId);
        verify(userMapper, never()).toDTO(any());
    }

    @Test
    public void getUsersByIds_shouldReturnUserDTOs_whenUsersExist(){
        List<Long> userIds = Arrays.asList(1L, 2L);
        User userEntity1 = new User();
        userEntity1.setId(1L);
        User userEntity2 = new User();
        userEntity2.setId(2L);

        List<User> userEntities = Arrays.asList(userEntity1, userEntity2);

        UserResponseDTO userResponseDTO1 = new UserResponseDTO();
        userResponseDTO1.setId(1L);

        UserResponseDTO userResponseDTO2 = new UserResponseDTO();
        userResponseDTO2.setId(2L);

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
    public void getUsersByIds_shouldReturnUserDTOs_whenNotAllUsersExist(){
        List<Long> userIds = Arrays.asList(1L, 2L);
        User userEntity1 = new User();
        userEntity1.setId(1L);

        List<User> userEntities = Arrays.asList(userEntity1);

        UserResponseDTO userResponseDTO1 = new UserResponseDTO();
        userResponseDTO1.setId(1L);

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
    public void getUsersByIds_shouldReturnEmptyList_whenNoUsersExist(){
        List<Long> userIds = Arrays.asList(1L, 2L);
        when(userRepository.findUsersByIds(userIds)).thenReturn(Collections.emptyList());

        List<UserResponseDTO> result = userService.getUsersByIds(userIds);

        assertNotNull(result);
        assertEquals(0, result.size());

        verify(userRepository).findUsersByIds(userIds);
        verify(userMapper, never()).toDTO(any(User.class));
    }

    @Test
    public void getUserByEmail_shouldReturnUserDTO_whenUserExists(){
        String email = "test@test.com";
        User userEntity = new User();
        userEntity.setEmail(email);

        UserResponseDTO userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(1L);
        userResponseDTO.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(userEntity));
        when(userMapper.toDTO(userEntity)).thenReturn(userResponseDTO);

        UserResponseDTO result = userService.getUserByEmail(email);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(email, result.getEmail());

        verify(userRepository).findByEmail(email);
        verify(userMapper).toDTO(userEntity);
    }

    @Test
    public void getUserByEmail_shouldThrowResourceNotFoundException_whenUserDoesntExists(){
        String email = "test@test.com";
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserByEmail(email);
        });

        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findByEmail(email);
        verify(userMapper, never()).toDTO(any());
    }

    @Test
    public void updateUser_shouldReturnUpdatedUserDTO_whenUserExists(){
        Long userId = 1L;
        UserRequestDTO userRequestDTO = new UserRequestDTO();
        userRequestDTO.setEmail("new@test.com");
        userRequestDTO.setName("newName");
        userRequestDTO.setSurname("newSurname");
        userRequestDTO.setBirthDate(LocalDate.of(2000, 12, 31));

        User userEntity = new User();
        userEntity.setId(userId);
        userEntity.setEmail("old@test.com");
        userEntity.setName("oldName");
        userEntity.setSurname("oldSurname");
        userEntity.setBirthDate(LocalDate.of(1990, 2, 2));

        User updatedUserEntity = new User();
        updatedUserEntity.setId(userId);
        updatedUserEntity.setEmail(userRequestDTO.getEmail());
        updatedUserEntity.setName(userRequestDTO.getName());
        updatedUserEntity.setSurname(userRequestDTO.getSurname());
        updatedUserEntity.setBirthDate(userRequestDTO.getBirthDate());

        UserResponseDTO userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(userId);
        userResponseDTO.setEmail(userRequestDTO.getEmail());
        userResponseDTO.setName(userRequestDTO.getName());
        userResponseDTO.setSurname(userRequestDTO.getSurname());
        userResponseDTO.setBirthDate(userRequestDTO.getBirthDate());

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
    public void updateUser_shouldThrowResourceNotFoundException_whenUserDoesntExists(){
        Long userId = 1L;
        UserRequestDTO userRequestDTO = new UserRequestDTO();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateUser(userId, userRequestDTO);
        });

        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findById(userId);
        verify(userRepository, never()).existsByEmail(any());
        verify(userMapper, never()).updateEntity(any(), any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toDTO(any());
    }

    @Test
    public void updateUser_shouldThrowConflictException_whenEmailAlreadyExists(){
        Long userId = 1L;
        UserRequestDTO userRequestDTO = new UserRequestDTO();
        userRequestDTO.setEmail("new@test.com");

        User userEntity = new User();
        userEntity.setId(userId);
        userEntity.setEmail("old@test.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userRepository.existsByEmail(userRequestDTO.getEmail())).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            userService.updateUser(userId, userRequestDTO);
        });

        assertEquals("User with email new@test.com already exists", exception.getMessage());

        verify(userRepository).findById(userId);
        verify(userRepository).existsByEmail(userRequestDTO.getEmail());
        verify(userMapper, never()).updateEntity(any(), any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toDTO(any());
    }

    @Test
    public void deleteUser_shouldDeleteUser(){
        Long userId = 1L;

        doNothing().when(userRepository).deleteById(userId);

        userService.deleteUser(userId);

        verify(userRepository).deleteById(userId);
    }

}
