package com.example.userservice.integration.EndToEnd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import com.redis.testcontainers.RedisContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.entities.User;
import com.example.userservice.entities.CardInfo;
import com.example.userservice.TestUtils;
import com.example.userservice.dto.CardInfoRequestDTO;
import com.example.userservice.dto.CardInfoResponseDTO;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CardInfoEndToEndIT {

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
    CardInfo cardInfo1;
    CardInfo cardInfo2;

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();

        user1 = TestUtils.createUser("John", "Doe", LocalDate.of(1990, 1, 1), "test1@example.com");

        cardInfo1 = TestUtils.createCardInfo("1111222233334444", "JOHN DOE", LocalDate.of(2025, 1, 1), user1);
        cardInfo2 = TestUtils.createCardInfo("2222333344445555", "JANE DOE", LocalDate.of(2025, 1, 1), user1);

        user1.setCardInfos(new ArrayList<>(List.of(cardInfo1, cardInfo2)));
        userRepository.saveAndFlush(user1);
    }

    @Test
    public void createCardInfo_shouldReturnCreatedCardInfo() {
        String url = "/api/card-infos";
        CardInfoRequestDTO cardInfoRequestDTO = TestUtils.createCardInfoRequestDTO("3333444455556666", "JOHN DOE", LocalDate.of(2027, 1, 1), user1.getId());
        
        ResponseEntity<CardInfoResponseDTO> response = testRestTemplate.exchange(
            url,
            HttpMethod.POST,
            new HttpEntity<>(cardInfoRequestDTO),
            CardInfoResponseDTO.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(cardInfoRequestDTO.getNumber(), response.getBody().getNumber());
        assertEquals(cardInfoRequestDTO.getHolder(), response.getBody().getHolder());
        assertEquals(cardInfoRequestDTO.getExpirationDate(), response.getBody().getExpirationDate());
        assertEquals(cardInfoRequestDTO.getUserId(), response.getBody().getUserId());
    }

    @Test
    public void createCardInfo_shouldReturnBadRequest_whenCardInfoAlreadyExists() {
        String url = "/api/card-infos";
        CardInfoRequestDTO cardInfoRequestDTO = TestUtils.createCardInfoRequestDTO("1111222233334444", "JOHN DOE", LocalDate.of(2027, 1, 1), user1.getId());
        
        ResponseEntity<Map<String, Object>> response = testRestTemplate.exchange(
            url,    
            HttpMethod.POST,
            new HttpEntity<>(cardInfoRequestDTO),
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Card info with number 1111222233334444 already exists", response.getBody().get("message"));
    }

    @Test
    public void getCardInfoById_shouldReturnCardInfo() {
        String url = String.format("/api/card-infos/%d", cardInfo1.getId());
        ResponseEntity<CardInfoResponseDTO> response = testRestTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            CardInfoResponseDTO.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(cardInfo1.getId(), response.getBody().getId());
        assertEquals(cardInfo1.getNumber(), response.getBody().getNumber());
        assertEquals(cardInfo1.getHolder(), response.getBody().getHolder());
        assertEquals(cardInfo1.getExpirationDate(), response.getBody().getExpirationDate());
        assertEquals(cardInfo1.getUser().getId(), response.getBody().getUserId());
    }

    @Test
    public void getCardInfoById_shouldReturnNotFound_whenCardInfoDoesNotExist() {
        String url = "/api/card-infos/999";
        ResponseEntity<Map<String, Object>> response = testRestTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Card info not found", response.getBody().get("message"));
    }

    @Test
    public void getCardInfosByIds_shouldReturnCardInfos() {
        String url = String.format("/api/card-infos/ids?ids=%d&ids=%d", cardInfo1.getId(), cardInfo2.getId());
        ResponseEntity<List<CardInfoResponseDTO>> response = testRestTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<CardInfoResponseDTO>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(cardInfo1.getId(), response.getBody().get(0).getId());
        assertEquals(cardInfo1.getNumber(), response.getBody().get(0).getNumber());
        assertEquals(cardInfo2.getId(), response.getBody().get(1).getId());
        assertEquals(cardInfo2.getNumber(), response.getBody().get(1).getNumber());
    }

    @Test
    public void getCardInfosByIds_shouldReturnEmptyList_whenNoCardInfosFound() {
        String url = "/api/card-infos/ids?ids=999&ids=1000";
        ResponseEntity<List<CardInfoResponseDTO>> response = testRestTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<CardInfoResponseDTO>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
    }

    @Test
    public void updateCardInfo_shouldReturnUpdatedCardInfo() {
        String url = String.format("/api/card-infos/%d", cardInfo1.getId());
        CardInfoRequestDTO cardInfoRequestDTO = TestUtils.createCardInfoRequestDTO("3333444455556666", "JOHN DOE", LocalDate.of(2030, 12, 31), user1.getId());
        ResponseEntity<CardInfoResponseDTO> response = testRestTemplate.exchange(
            url,
            HttpMethod.PUT,
            new HttpEntity<>(cardInfoRequestDTO),
            CardInfoResponseDTO.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(cardInfoRequestDTO.getNumber(), response.getBody().getNumber());
        assertEquals(cardInfoRequestDTO.getHolder(), response.getBody().getHolder());
        assertEquals(cardInfoRequestDTO.getExpirationDate(), response.getBody().getExpirationDate());
        assertEquals(cardInfoRequestDTO.getUserId(), response.getBody().getUserId());
    }

    @Test
    public void updateCardInfo_shouldReturnConflict_whenCardInfoAlreadyExists() {
        String url = String.format("/api/card-infos/%d", cardInfo1.getId());
        CardInfoRequestDTO cardInfoRequestDTO = TestUtils.createCardInfoRequestDTO("2222333344445555", "JOHN DOE", LocalDate.of(2030, 12, 31), user1.getId());
        ResponseEntity<Map<String, Object>> response = testRestTemplate.exchange(
            url,
            HttpMethod.PUT,
            new HttpEntity<>(cardInfoRequestDTO),
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Card info with number 2222333344445555 already exists", response.getBody().get("message"));
    }

    @Test
    public void deleteCardInfo_shouldReturnNoContent() {
        String url = String.format("/api/card-infos/%d", cardInfo1.getId());
        ResponseEntity<Void> response = testRestTemplate.exchange(
            url,
            HttpMethod.DELETE,
            null,
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
