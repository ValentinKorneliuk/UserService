package com.example.userservice.integration.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import com.example.userservice.entities.CardInfo;
import com.example.userservice.entities.User;
import com.example.userservice.repositories.CardInfoRepository;
import com.example.userservice.repositories.UserRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

import com.example.userservice.integration.BaseContainerIntegrationTest;

@ActiveProfiles("test")
@Sql(statements = {
        "TRUNCATE TABLE card_info RESTART IDENTITY CASCADE",
        "TRUNCATE TABLE users RESTART IDENTITY CASCADE"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DataJpaTest
class CardInfoRepositoryIntegrationTest extends BaseContainerIntegrationTest {

    @Autowired
    private CardInfoRepository cardInfoRepository;

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
                .surname("Smith")
                .birthDate(LocalDate.of(1995, 2, 2))
                .email("test2@example.com")
                .build();

        cardInfo1 = CardInfo.builder()
                .number("1111222233334444")
                .holder("John Doe")
                .expirationDate(LocalDate.of(2026, 1, 1))
                .user(user1)
                .build();
        cardInfo2 = CardInfo.builder()
                .number("2222333344445555")
                .holder("Jane Smith")
                .expirationDate(LocalDate.of(2026, 2, 2))
                .user(user2)
                .build();

        userRepository.saveAllAndFlush(new ArrayList<>(List.of(user1, user2)));
        cardInfoRepository.saveAllAndFlush(new ArrayList<>(List.of(cardInfo1, cardInfo2)));
    }

    @Test
    void saveCardInfo_shouldSaveCardInfo_whenCardInfoNumberUnique() {
        CardInfo cardInfo = new CardInfo();
        cardInfo.setNumber("3333444455556666");
        cardInfo.setHolder("John Doe");
        cardInfo.setExpirationDate(LocalDate.of(2027, 3, 3));
        cardInfo.setUser(user1);

        cardInfoRepository.save(cardInfo);

        CardInfo retrievedCardInfo = cardInfoRepository.findById(cardInfo.getId()).orElseThrow();
        assertEquals(cardInfo.getNumber(), retrievedCardInfo.getNumber());
        assertEquals(cardInfo.getHolder(), retrievedCardInfo.getHolder());
        assertEquals(cardInfo.getExpirationDate(), retrievedCardInfo.getExpirationDate());
        assertEquals(user1.getId(), retrievedCardInfo.getUser().getId());
    }

    @Test
    void saveCardInfo_shouldThrowException_whenCardInfoNumberNotUnique() {
        CardInfo cardInfo = new CardInfo();
        cardInfo.setNumber("1111222233334444");
        cardInfo.setHolder("Jane Smith");
        cardInfo.setExpirationDate(LocalDate.of(2026, 1, 1));
        cardInfo.setUser(user2);

        assertThrows(DataIntegrityViolationException.class, () -> cardInfoRepository.saveAndFlush(cardInfo));
    }

    @Test
    void updateCardInfo_shouldUpdateCardInfo_whenCardInfoNumberUnique() {
        String newNumber = "3333444455556666";
        cardInfo1.setNumber(newNumber);
        cardInfo1.setHolder("John Doe");
        cardInfo1.setExpirationDate(LocalDate.of(2027, 3, 3));

        cardInfoRepository.save(cardInfo1);

        CardInfo updatedCardInfo = cardInfoRepository.findById(cardInfo1.getId()).orElseThrow();

        assertEquals(newNumber, updatedCardInfo.getNumber());
    }

    @Test
    void updateCardInfo_shouldThrowException_whenCardInfoNumberAlreadyExists() {
        cardInfo1.setNumber("2222333344445555");
        cardInfo1.setHolder("John Doe");
        cardInfo1.setExpirationDate(LocalDate.of(2025, 1, 1));
        cardInfo1.setUser(user1);

        assertThrows(DataIntegrityViolationException.class, () -> cardInfoRepository.saveAndFlush(cardInfo1));
    }

    @Test
    void deleteCardInfo_shouldDeleteCardInfo_whenCardInfosExist() {
        UUID cardInfoId = cardInfo1.getId();
        cardInfoRepository.deleteById(cardInfoId);
        Optional<CardInfo> retrievedCardInfo = cardInfoRepository.findById(cardInfoId);
        assertEquals(true, retrievedCardInfo.isEmpty());
    }

    @Test
    void findCardsByIds_shouldReturnCardInfos_whenCardInfosExist() {
        UUID cardInfoId = cardInfo1.getId();
        CardInfo retrievedCardInfo = cardInfoRepository.findById(cardInfoId).orElseThrow();
        assertEquals(cardInfo1.getId(), retrievedCardInfo.getId());
        assertEquals(cardInfo1.getNumber(), retrievedCardInfo.getNumber());
        assertEquals(cardInfo1.getHolder(), retrievedCardInfo.getHolder());
        assertEquals(cardInfo1.getExpirationDate(), retrievedCardInfo.getExpirationDate());
        assertEquals(user1.getId(), retrievedCardInfo.getUser().getId());
    }

    @Test
    void existsByNumber_shouldReturnTrue_whenCardInfoExists() {
        String cardInfoNumber = cardInfo1.getNumber();
        boolean exists = cardInfoRepository.existsByNumber(cardInfoNumber);
        assertEquals(true, exists);
    }

    @Test
    void existsByNumber_shouldReturnFalse_whenCardInfoDoesNotExist() {
        String cardInfoNumber = "5555666677778888";
        boolean exists = cardInfoRepository.existsByNumber(cardInfoNumber);
        assertEquals(false, exists);
    }
}