package com.example.userservice.integration.EndToEnd;

import com.example.userservice.dto.CreateUserRequestDTO;
import com.example.userservice.dto.UpdateUserRequestDTO;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.entities.User;
import com.example.userservice.entities.CardInfo;
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.integration.BaseContainerIntegrationTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.core.ParameterizedTypeReference;

import static com.example.userservice.exceptions.ConflictException.USER_ALREADY_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

@ActiveProfiles("test")
@Sql(statements = {
        "TRUNCATE TABLE card_info RESTART IDENTITY CASCADE",
        "TRUNCATE TABLE users RESTART IDENTITY CASCADE"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserEndToEndIntegrationTest extends BaseContainerIntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "testpass");
    }

    User user1;
    User user2;
    CardInfo cardInfo1;
    CardInfo cardInfo2;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .name("John")
                .surname("Doe")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("test1@example.com")
                .build();
        user2 = User.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .name("Jane")
                .surname("Doe")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("test2@example.com")
                .build();
        cardInfo1 = CardInfo.builder()
                .number("1111222233334444")
                .holder("John Doe")
                .expirationDate(LocalDate.of(2026, 1, 1))
                .user(user1)
                .build();
        cardInfo2 = CardInfo.builder()
                .number("1111222233334445")
                .holder("Jane Doe")
                .expirationDate(LocalDate.of(2026, 1, 1))
                .user(user2)
                .build();
        user1.setCardInfos(new ArrayList<>(List.of(cardInfo1)));
        user2.setCardInfos(new ArrayList<>(List.of(cardInfo2)));
        userRepository.saveAllAndFlush(List.of(user1, user2));
    }


    @Test
    void createUser_shouldReturnCreatedUser() {
        CreateUserRequestDTO createUserRequestDTO = CreateUserRequestDTO.builder()
                .id(UUID.randomUUID())
                .name("Adam")
                .surname("Smith")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("test3@example.com")
                .build();
        String url = "/api/users";

        ResponseEntity<UserResponseDTO> response = testRestTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(createUserRequestDTO),
                UserResponseDTO.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(createUserRequestDTO.getEmail(), response.getBody().getEmail());
        assertEquals(createUserRequestDTO.getName(), response.getBody().getName());
        assertEquals(createUserRequestDTO.getSurname(), response.getBody().getSurname());
        assertEquals(createUserRequestDTO.getBirthDate(), response.getBody().getBirthDate());
    }

    @Test
    void createUser_shouldReturnBadRequest_whenEmailAlreadyExists() {
        CreateUserRequestDTO createUserRequestDTO = CreateUserRequestDTO.builder()
                .id(UUID.randomUUID())
                .name("John")
                .surname("Doe")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("test1@example.com")
                .build();
        String url = "/api/users";

        ResponseEntity<Map<String, Object>> response = testRestTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(createUserRequestDTO),
                new ParameterizedTypeReference<Map<String, Object>>() {
                }
        );

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(String.format(USER_ALREADY_EXISTS, createUserRequestDTO.getEmail()), response.getBody().get("message"));
    }

    @Test
    void getUserById_shouldReturnUser() {
        String url = String.format("/api/users/%s", user1.getId().toString());

        ResponseEntity<UserResponseDTO> response = testRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                UserResponseDTO.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(user1.getId(), response.getBody().getId());
        assertEquals("test1@example.com", response.getBody().getEmail());
    }

    @Test
    void getUsersByIds_shouldReturnUsers() {
        String url = String.format("/api/users/ids?ids=%s&ids=%s", user1.getId().toString(), user2.getId().toString());
        ResponseEntity<List<UserResponseDTO>> response = testRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<UserResponseDTO>>() {
                }
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(user1.getId(), response.getBody().get(0).getId());
        assertEquals(user1.getCardInfos().size(), response.getBody().get(0).getCardInfos().size());
        assertEquals(user2.getId(), response.getBody().get(1).getId());
        assertEquals(user2.getCardInfos().size(), response.getBody().get(1).getCardInfos().size());
    }

    @Test
    void getUsersByIds_shouldReturnEmptyList_whenNoUsersFound() {
        String url = "/api/users/ids?ids=00000000-0000-0000-0000-000000000999&ids=00000000-0000-0000-0000-000000001000";
        ResponseEntity<List<UserResponseDTO>> response = testRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<UserResponseDTO>>() {
                }
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
    }

    @Test
    void updateUser_shouldReturnUpdatedUser() {
        String url = String.format("/api/users/%s", user1.getId().toString());
        UpdateUserRequestDTO updateUserRequestDTO = UpdateUserRequestDTO.builder()
                .name("Adam")
                .surname("Smith")
                .birthDate(LocalDate.of(2000, 12, 31))
                .email("test3@example.com")
                .build();
        ResponseEntity<UserResponseDTO> response = testRestTemplate.exchange(
                url,
                HttpMethod.PUT,
                new HttpEntity<>(updateUserRequestDTO),
                UserResponseDTO.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(updateUserRequestDTO.getEmail(), response.getBody().getEmail());
        assertEquals(updateUserRequestDTO.getName(), response.getBody().getName());
        assertEquals(updateUserRequestDTO.getSurname(), response.getBody().getSurname());
        assertEquals(updateUserRequestDTO.getBirthDate(), response.getBody().getBirthDate());
    }

    @Test
    void updateUser_shouldReturnConflict_whenEmailAlreadyExists() {
        String url = String.format("/api/users/%s", user1.getId().toString());
        UpdateUserRequestDTO updateUserRequestDTO = UpdateUserRequestDTO.builder()
                .name("Adam")
                .surname("Smith")
                .birthDate(LocalDate.of(2000, 12, 31))
                .email("test2@example.com")
                .build();
        ResponseEntity<Map<String, Object>> response = testRestTemplate.exchange(
                url,
                HttpMethod.PUT,
                new HttpEntity<>(updateUserRequestDTO),
                new ParameterizedTypeReference<Map<String, Object>>() {
                }
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(
                String.format(USER_ALREADY_EXISTS, updateUserRequestDTO.getEmail()),
                response.getBody().get("message")
        );
    }

    @Test
    void deleteUser_shouldReturnNoContent() {
        String url = String.format("/api/users/%s", user1.getId().toString());
        ResponseEntity<Void> response = testRestTemplate.exchange(
                url,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

}