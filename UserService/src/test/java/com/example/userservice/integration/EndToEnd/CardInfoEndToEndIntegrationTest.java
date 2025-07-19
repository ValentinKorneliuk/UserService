package com.example.userservice.integration.EndToEnd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import com.example.userservice.repositories.UserRepository;
import com.example.userservice.entities.User;
import com.example.userservice.entities.CardInfo;
import com.example.userservice.dto.CardInfoRequestDTO;
import com.example.userservice.dto.CardInfoResponseDTO;
import com.example.userservice.integration.BaseContainerIntegrationTest;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpStatus;

import static com.example.userservice.exceptions.ConflictException.CARD_INFO_ALREADY_EXISTS;
import static com.example.userservice.exceptions.ResourceNotFoundException.CARD_INFO_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
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
class CardInfoEndToEndIntegrationTest extends BaseContainerIntegrationTest {

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
    CardInfo cardInfo1;
    CardInfo cardInfo2;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
            .name("John")
            .surname("Doe")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("test1@example.com")
            .build();

        cardInfo1 = CardInfo.builder()
            .number("1111222233334444")
            .holder("John Doe")
            .expirationDate(LocalDate.of(2026, 1, 1))
            .user(user1)
            .build();
        cardInfo2 = CardInfo.builder()
            .number("2222333344445555")
            .holder("Jane Doe")
            .expirationDate(LocalDate.of(2026, 1, 1))
            .user(user1)
            .build();

        user1.setCardInfos(new ArrayList<>(List.of(cardInfo1, cardInfo2)));
        userRepository.saveAndFlush(user1);
    }

    @Test
    void createCardInfo_shouldReturnCreatedCardInfo() {
        String url = "/api/card-infos";
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("3333444455556666")
            .holder("JOHN DOE")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .userId(user1.getId())
            .build();
        
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
    void createCardInfo_shouldReturnBadRequest_whenCardInfoAlreadyExists() {
        String url = "/api/card-infos";
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("1111222233334444")
            .holder("JOHN DOE")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .userId(user1.getId())
            .build();
        
        ResponseEntity<Map<String, Object>> response = testRestTemplate.exchange(
            url,    
            HttpMethod.POST,
            new HttpEntity<>(cardInfoRequestDTO),
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(String.format(CARD_INFO_ALREADY_EXISTS, cardInfoRequestDTO.getNumber()), response.getBody().get("message"));
    }

    @Test
    void getCardInfoById_shouldReturnCardInfo() {
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
    void getCardInfoById_shouldReturnNotFound_whenCardInfoDoesNotExist() {
        String url = "/api/card-infos/999";
        ResponseEntity<Map<String, Object>> response = testRestTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(CARD_INFO_NOT_FOUND, response.getBody().get("message"));
    }

    @Test
    void getCardInfosByIds_shouldReturnCardInfos() {
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
    void getCardInfosByIds_shouldReturnEmptyList_whenNoCardInfosFound() {
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
    void updateCardInfo_shouldReturnUpdatedCardInfo() {
        String url = String.format("/api/card-infos/%d", cardInfo1.getId());
            CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("3333444455556666")
            .holder("JOHN DOE")
            .expirationDate(LocalDate.of(2030, 12, 31))
            .userId(user1.getId())
            .build();
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
    void updateCardInfo_shouldReturnConflict_whenCardInfoAlreadyExists() {
        String url = String.format("/api/card-infos/%d", cardInfo1.getId());
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("2222333344445555")
            .holder("JOHN DOE")
            .expirationDate(LocalDate.of(2030, 12, 31))
            .userId(user1.getId())
            .build();
        ResponseEntity<Map<String, Object>> response = testRestTemplate.exchange(
            url,
            HttpMethod.PUT,
            new HttpEntity<>(cardInfoRequestDTO),
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(String.format(CARD_INFO_ALREADY_EXISTS, cardInfoRequestDTO.getNumber()), response.getBody().get("message"));
    }

    @Test
    void deleteCardInfo_shouldReturnNoContent() {
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
