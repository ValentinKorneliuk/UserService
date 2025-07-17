package com.example.userservice.integration.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.hasItem;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import com.example.userservice.controllers.UserController;
import com.example.userservice.dto.UserRequestDTO;
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.services.UserService;
import com.example.userservice.exceptions.ResourceNotFoundException;
import com.example.userservice.exceptions.ConflictException;

@WebMvcTest(UserController.class)
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void getUserById_shouldReturnUser() throws Exception {
        Long userId = 1L;
        UserResponseDTO userResponseDTO = UserResponseDTO.builder()
            .id(userId)
            .name("John")
            .surname("Doe")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("john.doe@example.com")
            .build();

        when(userService.getUserById(userId)).thenReturn(userResponseDTO);
        
        mockMvc.perform(get("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.name").value("John"))
            .andExpect(jsonPath("$.surname").value("Doe"))
            .andExpect(jsonPath("$.birthDate").value("1990-01-01"))
            .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    public void getUserById_shouldReturnNotFound() throws Exception {
        Long userId = 1L;

        when(userService.getUserById(userId)).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User not found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not found"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/users/1"));
    }

    @Test
    public void getUsersByIds_shouldReturnUsers() throws Exception {
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        List<UserResponseDTO> userResponseDTOs = new ArrayList<>();
        userResponseDTOs.add(UserResponseDTO.builder()
            .id(1L)
            .name("John")
            .surname("Doe")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("john.doe@example.com")
            .build());
        userResponseDTOs.add(UserResponseDTO.builder()
            .id(2L)
            .name("Jane")
            .surname("Smith")
            .birthDate(LocalDate.of(1991, 2, 2))
            .email("jane.smith@example.com")
            .build());
        userResponseDTOs.add(UserResponseDTO.builder()
            .id(3L)
            .name("Jim")
            .surname("Beam")
            .birthDate(LocalDate.of(1992, 3, 3))
            .email("jim.beam@example.com")
            .build());

        when(userService.getUsersByIds(userIds)).thenReturn(userResponseDTOs);

        mockMvc.perform(get("/api/users/ids")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .param("ids", userIds.stream().map(String::valueOf).collect(Collectors.joining(","))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size()").value(userResponseDTOs.size()))
            .andExpect(jsonPath("$[0].id").value(userResponseDTOs.get(0).getId()))
            .andExpect(jsonPath("$[0].name").value(userResponseDTOs.get(0).getName()))
            .andExpect(jsonPath("$[0].surname").value(userResponseDTOs.get(0).getSurname()))
            .andExpect(jsonPath("$[0].birthDate").value(userResponseDTOs.get(0).getBirthDate().toString()))
            .andExpect(jsonPath("$[0].email").value(userResponseDTOs.get(0).getEmail()));
    }

    @Test
    public void getUsersByIds_shouldReturnEmptyList() throws Exception {
        List<Long> userIds = Arrays.asList();
        when(userService.getUsersByIds(userIds)).thenReturn(new ArrayList<>());
        
        mockMvc.perform(get("/api/users/ids")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .param("ids", userIds.stream().map(String::valueOf).collect(Collectors.joining(","))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size()").value(0));
    }

    @Test
    public void getUsersByIds_shouldReturnBadRequestOnAbsentIds() throws Exception {
        mockMvc.perform(get("/api/users/ids")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("ids parameter is missing"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Missing request parameter"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/users/ids"));
    }

    @Test
    public void createUser_shouldReturnUser() throws Exception {
        UserRequestDTO userRequestDTO = UserRequestDTO.builder()
            .name("John")
            .surname("Doe")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("john.doe@example.com")
            .build();
        UserResponseDTO userResponseDTO = UserResponseDTO.builder()
            .id(1L)
            .name("John")
            .surname("Doe")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("john.doe@example.com")
            .build();

        when(userService.createUser(any(UserRequestDTO.class))).thenReturn(userResponseDTO);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequestDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("John"))
            .andExpect(jsonPath("$.surname").value("Doe"))
            .andExpect(jsonPath("$.birthDate").value("1990-01-01"))
            .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    public void createUser_shouldReturnBadRequest_whenBlankFieldsAndNullBirthDateAndInvalidEmail() throws Exception {
        UserRequestDTO userRequestDTO = UserRequestDTO.builder()
            .name("")
            .surname(" ")
            .birthDate(null)
            .email(" ")
            .build();
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequestDTO)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.messages", hasItem("name: Name must not be blank")))
            .andExpect(jsonPath("$.messages", hasItem("surname: Surname must not be blank")))
            .andExpect(jsonPath("$.messages", hasItem("birthDate: Birth date is required")))
            .andExpect(jsonPath("$.messages", hasItem("email: Email must not be blank")))
            .andExpect(jsonPath("$.messages", hasItem("email: Email should be valid")))
            .andExpect(jsonPath("$.messages", hasItem("name: Name must be between 2 and 100 characters")))
            .andExpect(jsonPath("$.messages", hasItem("surname: Surname must be between 2 and 100 characters")))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/users"));
    }

    @Test
    public void createUser_shouldReturnBadRequest_whenTooLongFieldsAndInvalidEmail() throws Exception {
        UserRequestDTO userRequestDTO = UserRequestDTO.builder()
            .name("JohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohn")
            .surname("DoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoe")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("john.doe@example.comjohn.doe@example.comjohn.doe@example.comjohn.doe@example.comjohn.doe@example.comjohn.doe@example.com")
            .build();

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequestDTO)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.messages", hasItem("name: Name must be between 2 and 100 characters")))
            .andExpect(jsonPath("$.messages", hasItem("surname: Surname must be between 2 and 100 characters")))
            .andExpect(jsonPath("$.messages", hasItem("email: Email must be less than 100 characters")))
            .andExpect(jsonPath("$.messages", hasItem("email: Email should be valid")))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/users"));
    }

    @Test
    public void createUser_shouldReturnConflict_whenEmailAlreadyExists() throws Exception {
        UserRequestDTO userRequestDTO = UserRequestDTO.builder()
            .name("John")
            .surname("Doe")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("john.doe@example.com")
            .build();
        
        when(userService.createUser(any(UserRequestDTO.class))).thenThrow(new ConflictException("Email already exists"));

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequestDTO)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Email already exists"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/users"));
    }

    @Test
    public void updateUser_shouldReturnUpdatedUser() throws Exception {
        Long userId = 1L;
        UserRequestDTO userRequestDTO = UserRequestDTO.builder()
            .name("John")
            .surname("Doe")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("john.doe@example.com")
            .build();
        UserResponseDTO userResponseDTO = UserResponseDTO.builder()
            .id(userId)
            .name("John")
            .surname("Doe")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("john.doe@example.com")
            .build();

        when(userService.updateUser(eq(userId), any(UserRequestDTO.class))).thenReturn(userResponseDTO);

        mockMvc.perform(put("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequestDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.name").value("John"))
            .andExpect(jsonPath("$.surname").value("Doe"))
            .andExpect(jsonPath("$.birthDate").value("1990-01-01"))
            .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    public void updateUser_shouldReturnBadRequest_whenBlankFieldsAndNullBirthDateAndInvalidEmail() throws Exception {
        Long userId = 1L;
        UserRequestDTO userRequestDTO = UserRequestDTO.builder()
            .name("")
            .surname(" ")
            .birthDate(null)
            .email(" ")
            .build();
        
        mockMvc.perform(put("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequestDTO)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.messages", hasItem("name: Name must not be blank")))
            .andExpect(jsonPath("$.messages", hasItem("surname: Surname must not be blank")))
            .andExpect(jsonPath("$.messages", hasItem("birthDate: Birth date is required")))
            .andExpect(jsonPath("$.messages", hasItem("email: Email must not be blank")))
            .andExpect(jsonPath("$.messages", hasItem("email: Email should be valid")))
            .andExpect(jsonPath("$.messages", hasItem("name: Name must be between 2 and 100 characters")))
            .andExpect(jsonPath("$.messages", hasItem("surname: Surname must be between 2 and 100 characters")))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/users/" + userId));
    }

    @Test
    public void updateUser_shouldReturnBadRequest_whenTooLongFieldsAndInvalidEmail() throws Exception {
        Long userId = 1L;
        UserRequestDTO userRequestDTO = UserRequestDTO.builder()
            .name("JohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohn")
            .surname("DoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoe")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("john.doe@example.comjohn.doe@example.comjohn.doe@example.comjohn.doe@example.comjohn.doe@example.comjohn.doe@example.com")
            .build();

        mockMvc.perform(put("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequestDTO)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.messages", hasItem("name: Name must be between 2 and 100 characters")))
            .andExpect(jsonPath("$.messages", hasItem("surname: Surname must be between 2 and 100 characters")))
            .andExpect(jsonPath("$.messages", hasItem("email: Email must be less than 100 characters")))
            .andExpect(jsonPath("$.messages", hasItem("email: Email should be valid")))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/users/" + userId));
    }

    @Test
    public void updateUser_shouldReturnConflict_whenEmailAlreadyExists() throws Exception {
        Long userId = 1L;
        UserRequestDTO userRequestDTO = UserRequestDTO.builder()
            .name("John")
            .surname("Doe")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("john.doe@example.com")
            .build();
        
        when(userService.updateUser(eq(userId), any(UserRequestDTO.class))).thenThrow(new ConflictException("Email already exists"));

        mockMvc.perform(put("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequestDTO)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Email already exists"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/users/" + userId));
    }

    @Test
    public void updateUser_shouldReturnResourceNotFoundException_whenUserNotFound() throws Exception {
        Long userId = 1L;
        UserRequestDTO userRequestDTO = UserRequestDTO.builder()
            .name("John")
            .surname("Doe")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("john.doe@example.com")
            .build();
        
        when(userService.updateUser(eq(userId), any(UserRequestDTO.class))).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(put("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequestDTO)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not found"))
            .andExpect(jsonPath("$.message").value("User not found"))
            .andExpect(jsonPath("$.path").value("/api/users/" + userId));
    }   

    @Test
    public void deleteUser_shouldReturnNoContent() throws Exception {
        Long userId = 1L;
        doNothing().when(userService).deleteUser(eq(userId));
        
        mockMvc.perform(delete("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());
    }

}
