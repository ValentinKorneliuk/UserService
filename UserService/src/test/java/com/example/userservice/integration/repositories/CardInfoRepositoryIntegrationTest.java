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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

import com.example.userservice.integration.BaseContainerIntegrationTest;

@Sql(statements = {
    "TRUNCATE TABLE card_info RESTART IDENTITY CASCADE",
    "TRUNCATE TABLE users RESTART IDENTITY CASCADE"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DataJpaTest
public class CardInfoRepositoryIntegrationTest extends BaseContainerIntegrationTest {

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
            .name("John")
            .surname("Doe")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("test1@example.com")
            .build();
        user2 = User.builder()
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
    public void saveCardInfo_shouldSaveCardInfo_whenCardInfoNumberUnique() {
        CardInfo cardInfo = new CardInfo();
        cardInfo.setNumber("3333444455556666");
        cardInfo.setHolder("John Doe");
        cardInfo.setExpirationDate(LocalDate.of(2027, 3, 3));
        cardInfo.setUser(user1);

        cardInfoRepository.save(cardInfo);

        CardInfo retrievedCardInfo = cardInfoRepository.findById(cardInfo.getId()).orElseThrow();
        assertEquals(retrievedCardInfo.getNumber(), cardInfo.getNumber());
        assertEquals(retrievedCardInfo.getHolder(), cardInfo.getHolder());
        assertEquals(retrievedCardInfo.getExpirationDate(), cardInfo.getExpirationDate());
        assertEquals(retrievedCardInfo.getUser().getId(), user1.getId());
    }

    @Test
    public void saveCardInfo_shouldThrowException_whenCardInfoNumberNotUnique() {
        CardInfo cardInfo = new CardInfo();
        cardInfo.setNumber("1111222233334444");
        cardInfo.setHolder("Jane Smith");
        cardInfo.setExpirationDate(LocalDate.of(2025, 1, 1));
        cardInfo.setUser(user2);

        assertThrows(DataIntegrityViolationException.class, () -> cardInfoRepository.save(cardInfo));
    }

    @Test
    public void updateCardInfo_shouldUpdateCardInfo_whenCardInfoNumberUnique() {
        cardInfo1.setNumber("3333444455556666");
        cardInfo1.setHolder("John Doe");
        cardInfo1.setExpirationDate(LocalDate.of(2027, 3, 3));

        cardInfoRepository.save(cardInfo1);

        CardInfo updatedCardInfo = cardInfoRepository.findById(cardInfo1.getId()).orElseThrow();

        assertEquals(updatedCardInfo.getNumber(), updatedCardInfo.getNumber());
    }

    @Test
    public void updateCardInfo_shouldThrowException_whenCardInfoNumberAlreadyExists() {
        cardInfo1.setNumber("2222333344445555");
        cardInfo1.setHolder("John Doe");
        cardInfo1.setExpirationDate(LocalDate.of(2025, 1, 1));
        cardInfo1.setUser(user1);

        assertThrows(DataIntegrityViolationException.class, () -> cardInfoRepository.saveAndFlush(cardInfo1));
    }

    @Test
    public void deleteCardInfo_shouldDeleteCardInfo_whenCardInfosExist() {
        Long cardInfoId = cardInfo1.getId();
        cardInfoRepository.deleteById(cardInfoId);
        Optional<CardInfo> retrievedCardInfo = cardInfoRepository.findById(cardInfoId);
        assertEquals(retrievedCardInfo.isEmpty(), true);
    }

    @Test
    public void findCardsByIds_shouldReturnCardInfos_whenCardInfosExist() {
        Long cardInfoId = cardInfo1.getId();
        CardInfo retrievedCardInfo = cardInfoRepository.findById(cardInfoId).orElseThrow();
        assertEquals(retrievedCardInfo.getId(), cardInfo1.getId());
        assertEquals(retrievedCardInfo.getNumber(), cardInfo1.getNumber());
        assertEquals(retrievedCardInfo.getHolder(), cardInfo1.getHolder());
        assertEquals(retrievedCardInfo.getExpirationDate(), cardInfo1.getExpirationDate());
        assertEquals(retrievedCardInfo.getUser().getId(), user1.getId());
    }


    @Test
    public void existsByNumber_shouldReturnTrue_whenCardInfoExists() {
        String cardInfoNumber = cardInfo1.getNumber();
        boolean exists = cardInfoRepository.existsByNumber(cardInfoNumber);
        assertEquals(exists, true);
    }

    @Test
    public void existsByNumber_shouldReturnFalse_whenCardInfoDoesNotExist() {
        String cardInfoNumber = "5555666677778888";
        boolean exists = cardInfoRepository.existsByNumber(cardInfoNumber);
        assertEquals(exists, false);
    }
}
