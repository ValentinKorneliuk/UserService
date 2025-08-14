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
import java.util.UUID;

import com.example.userservice.dto.CreateUserRequestDTO;
import com.example.userservice.dto.UpdateUserRequestDTO;
import org.junit.jupiter.api.Test;

import static com.example.userservice.exceptions.ConflictException.USER_ALREADY_EXISTS;
import static com.example.userservice.exceptions.ResourceNotFoundException.USER_NOT_FOUND;
import static org.hamcrest.Matchers.hasItem;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import com.example.userservice.controllers.UserController;
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.exceptions.ConflictException;
import com.example.userservice.exceptions.ResourceNotFoundException;
import com.example.userservice.services.UserService;

@ActiveProfiles("test")
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getUserById_shouldReturnUser() throws Exception {
        UUID userId = UUID.randomUUID();
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
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.surname").value("Doe"))
                .andExpect(jsonPath("$.birthDate").value("1990-01-01"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void getUserById_shouldReturnNotFound() throws Exception {
        UUID userId = UUID.randomUUID();

        when(userService.getUserById(userId)).thenThrow(new ResourceNotFoundException(USER_NOT_FOUND));

        mockMvc.perform(get("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(USER_NOT_FOUND))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not found"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/users/" + userId));
    }

    @Test
    void getUsersByIds_shouldReturnUsers() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        List<UUID> userIds = Arrays.asList(id1, id2, id3);
        List<UserResponseDTO> userResponseDTOs = new ArrayList<>();
        userResponseDTOs.add(UserResponseDTO.builder()
                .id(id1)
                .name("John")
                .surname("Doe")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("john.doe@example.com")
                .build());
        userResponseDTOs.add(UserResponseDTO.builder()
                .id(id2)
                .name("Jane")
                .surname("Smith")
                .birthDate(LocalDate.of(1991, 2, 2))
                .email("jane.smith@example.com")
                .build());
        userResponseDTOs.add(UserResponseDTO.builder()
                .id(id3)
                .name("Jim")
                .surname("Beam")
                .birthDate(LocalDate.of(1992, 3, 3))
                .email("jim.beam@example.com")
                .build());

        when(userService.getUsersByIds(userIds)).thenReturn(userResponseDTOs);

        mockMvc.perform(get("/api/users/ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("ids", userIds.stream().map(UUID::toString).collect(Collectors.joining(","))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(userResponseDTOs.size()))
                .andExpect(jsonPath("$[0].id").value(userResponseDTOs.get(0).getId().toString()))
                .andExpect(jsonPath("$[0].name").value(userResponseDTOs.get(0).getName()))
                .andExpect(jsonPath("$[0].surname").value(userResponseDTOs.get(0).getSurname()))
                .andExpect(jsonPath("$[0].birthDate").value(userResponseDTOs.get(0).getBirthDate().toString()))
                .andExpect(jsonPath("$[0].email").value(userResponseDTOs.get(0).getEmail()));
    }

    @Test
    void getUsersByIds_shouldReturnEmptyList() throws Exception {
        List<UUID> userIds = Arrays.asList();
        when(userService.getUsersByIds(userIds)).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/users/ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("ids", userIds.stream().map(UUID::toString).collect(Collectors.joining(","))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(0));
    }

    @Test
    void getUsersByIds_shouldReturnBadRequestOnAbsentIds() throws Exception {
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
    void createUser_shouldReturnUser() throws Exception {
        UUID userId = UUID.randomUUID();
        CreateUserRequestDTO createUserRequestDTO = CreateUserRequestDTO.builder()
                .id(userId)
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

        when(userService.createUser(any(CreateUserRequestDTO.class))).thenReturn(userResponseDTO);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.surname").value("Doe"))
                .andExpect(jsonPath("$.birthDate").value("1990-01-01"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void createUser_shouldReturnBadRequest_whenBlankFieldsAndNullBirthDateAndInvalidEmail() throws Exception {
        CreateUserRequestDTO createUserRequestDTO = CreateUserRequestDTO.builder()
                .name("")
                .surname(" ")
                .birthDate(null)
                .email(" ")
                .build();

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.messages", hasItem("id: Id must not be blank")))
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
    void createUser_shouldReturnBadRequest_whenTooLongFieldsAndInvalidEmail() throws Exception {
        CreateUserRequestDTO createUserRequestDTO = CreateUserRequestDTO.builder()
                .name("JohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohn")
                .surname("DoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoe")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("john.doe@example.comjohn.doe@example.comjohn.doe@example.comjohn.doe@example.comjohn.doe@example.comjohn.doe@example.com")
                .build();

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequestDTO)))
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
    void createUser_shouldReturnConflict_whenEmailAlreadyExists() throws Exception {
        CreateUserRequestDTO createUserRequestDTO = CreateUserRequestDTO.builder()
                .id(UUID.randomUUID())
                .name("John")
                .surname("Doe")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("john.doe@example.com")
                .build();

        when(userService.createUser(any(CreateUserRequestDTO.class)))
                .thenThrow(new ConflictException(String.format(USER_ALREADY_EXISTS, createUserRequestDTO.getEmail())));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequestDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(String.format(USER_ALREADY_EXISTS, createUserRequestDTO.getEmail())))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/users"));
    }

    @Test
    void updateUser_shouldReturnUpdatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserRequestDTO updateUserRequestDTO = UpdateUserRequestDTO.builder()
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

        when(userService.updateUser(eq(userId), any(UpdateUserRequestDTO.class))).thenReturn(userResponseDTO);

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.surname").value("Doe"))
                .andExpect(jsonPath("$.birthDate").value("1990-01-01"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void updateUser_shouldReturnBadRequest_whenBlankFieldsAndNullBirthDateAndInvalidEmail() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserRequestDTO updateUserRequestDTO = UpdateUserRequestDTO.builder()
                .name("")
                .surname(" ")
                .birthDate(null)
                .email(" ")
                .build();

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequestDTO)))
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
    void updateUser_shouldReturnBadRequest_whenTooLongFieldsAndInvalidEmail() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserRequestDTO updateUserRequestDTO = UpdateUserRequestDTO.builder()
                .name("JohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohnJohn")
                .surname("DoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoe")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("john.doe@example.comjohn.doe@example.comjohn.doe@example.comjohn.doe@example.comjohn.doe@example.comjohn.doe@example.com")
                .build();

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequestDTO)))
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
    void updateUser_shouldReturnConflict_whenEmailAlreadyExists() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserRequestDTO updateUserRequestDTO = UpdateUserRequestDTO.builder()
                .name("John")
                .surname("Doe")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("john.doe@example.com")
                .build();

        when(userService.updateUser(eq(userId), any(UpdateUserRequestDTO.class)))
                .thenThrow(new ConflictException(String.format(USER_ALREADY_EXISTS, updateUserRequestDTO.getEmail())));

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequestDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(String.format(USER_ALREADY_EXISTS, updateUserRequestDTO.getEmail())))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/users/" + userId));
    }

    @Test
    void updateUser_shouldReturnResourceNotFoundException_whenUserNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserRequestDTO updateUserRequestDTO = UpdateUserRequestDTO.builder()
                .name("John")
                .surname("Doe")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("john.doe@example.com")
                .build();

        when(userService.updateUser(eq(userId), any(UpdateUserRequestDTO.class))).thenThrow(new ResourceNotFoundException(USER_NOT_FOUND));

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequestDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not found"))
                .andExpect(jsonPath("$.message").value(USER_NOT_FOUND))
                .andExpect(jsonPath("$.path").value("/api/users/" + userId));
    }

    @Test
    void deleteUser_shouldReturnNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

}