package com.example.userservice.integration.EndToEnd;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.utility.DockerImageName;
import com.redis.testcontainers.RedisContainer;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.entities.User;
import com.example.userservice.entities.CardInfo;
import com.example.userservice.TestUtils;
import com.example.userservice.dto.UserRequestDTO;
import com.example.userservice.dto.UserResponseDTO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List; 
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.core.ParameterizedTypeReference;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserEndToEndIT {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass");

    @Container
    private static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:8.0.2"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
    }

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UserRepository userRepository;


    User user1;
    User user2;
    CardInfo cardInfo1;
    CardInfo cardInfo2;

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();

        user1 = TestUtils.createUser("John", "Doe", LocalDate.of(1990, 1, 1), "test1@example.com");
        user2 = TestUtils.createUser("Jane", "Doe", LocalDate.of(1990, 1, 1), "test2@example.com");

        cardInfo1 = TestUtils.createCardInfo("1111222233334444", "JOHN DOE", LocalDate.of(2025, 1, 1), user1);
        cardInfo2 = TestUtils.createCardInfo("1111222233334445", "JANE DOE", LocalDate.of(2025, 1, 1), user2);

        user1.setCardInfos(new ArrayList<>(List.of(cardInfo1)));
        user2.setCardInfos(new ArrayList<>(List.of(cardInfo2)));
        userRepository.saveAllAndFlush(List.of(user1, user2));
    }

    @Test
    public void createUser_shouldReturnCreatedUser() {
        UserRequestDTO userRequestDTO = TestUtils.createUserRequestDTO("Adam", "Smith", LocalDate.of(1990, 1, 1), "test3@example.com");
        String url = "/api/users";

        ResponseEntity<UserResponseDTO> response = testRestTemplate.exchange(
            url,
            HttpMethod.POST,
            new HttpEntity<>(userRequestDTO),
            UserResponseDTO.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(userRequestDTO.getEmail(), response.getBody().getEmail());
        assertEquals(userRequestDTO.getName(), response.getBody().getName());
        assertEquals(userRequestDTO.getSurname(), response.getBody().getSurname());
        assertEquals(userRequestDTO.getBirthDate(), response.getBody().getBirthDate());
    }

    @Test
    public void createUser_shouldReturnBadRequest_whenEmailAlreadyExists() {
        UserRequestDTO userRequestDTO = TestUtils.createUserRequestDTO("John", "Doe", LocalDate.of(1990, 1, 1), "test1@example.com");
        String url = "/api/users";

        ResponseEntity<Map<String, Object>> response = testRestTemplate.exchange(
            url,
            HttpMethod.POST,
            new HttpEntity<>(userRequestDTO),
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("User with email test1@example.com already exists", response.getBody().get("message"));
    }

    @Test
    public void getUserById_shouldReturnUser() {
        String url = String.format("/api/users/%d", user1.getId());

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
    public void getUserById_shouldReturnNotFound_whenUserDoesNotExist() {
        String url = "/api/users/999";

        ResponseEntity<Map<String, Object>> response = testRestTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User not found", response.getBody().get("message"));
    }

    @Test
    public void getUsersByIds_shouldReturnUsers() {
        String url = String.format("/api/users/ids?ids=%d&ids=%d", user1.getId(), user2.getId());
        ResponseEntity<List<UserResponseDTO>> response = testRestTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<UserResponseDTO>>() {}
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
    public void getUsersByIds_shouldReturnEmptyList_whenNoUsersFound() {
        String url = "/api/users/ids?ids=999&ids=1000";
        ResponseEntity<List<UserResponseDTO>> response = testRestTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<UserResponseDTO>>() {}
        ); 

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
    }

    @Test
    public void updateUser_shouldReturnUpdatedUser() {
        String url = String.format("/api/users/%d", user1.getId());
        UserRequestDTO userRequestDTO = TestUtils.createUserRequestDTO("Adam", "Smith", LocalDate.of(2000, 12, 31), "test3@example.com");
        ResponseEntity<UserResponseDTO> response = testRestTemplate.exchange(
            url,
            HttpMethod.PUT,
            new HttpEntity<>(userRequestDTO),
            UserResponseDTO.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(userRequestDTO.getEmail(), response.getBody().getEmail());
        assertEquals(userRequestDTO.getName(), response.getBody().getName());
        assertEquals(userRequestDTO.getSurname(), response.getBody().getSurname());
        assertEquals(userRequestDTO.getBirthDate(), response.getBody().getBirthDate());
    }

    @Test
    public void updateUser_shouldReturnConflict_whenEmailAlreadyExists() {
        String url = String.format("/api/users/%d", user1.getId());
        UserRequestDTO userRequestDTO = TestUtils.createUserRequestDTO("Adam", "Smith", LocalDate.of(2000, 12, 31), "test2@example.com");
        ResponseEntity<Map<String, Object>> response = testRestTemplate.exchange(
            url,
            HttpMethod.PUT,
            new HttpEntity<>(userRequestDTO),
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User with email test2@example.com already exists", response.getBody().get("message"));
    }

    @Test
    public void deleteUser_shouldReturnNoContent() {
        String url = String.format("/api/users/%d", user1.getId());
        ResponseEntity<Void> response = testRestTemplate.exchange(
            url,
            HttpMethod.DELETE,
            null,
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

}
