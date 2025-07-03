package com.example.userservice.integration.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import com.example.userservice.TestUtils;
import com.example.userservice.entities.CardInfo;
import com.example.userservice.entities.User;
import com.example.userservice.repositories.CardInfoRepository;
import com.example.userservice.repositories.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@DataJpaTest
@Testcontainers
public class CardInfoRepositoryIT {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"))
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpassword");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        postgres.start();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CardInfoRepository cardInfoRepository;

    @Autowired
    private UserRepository userRepository;

    User user1;
    User user2;
    CardInfo cardInfo1;
    CardInfo cardInfo2;

    @BeforeEach
    void setUp() {
        user1 = TestUtils.createUser("John", "Doe", LocalDate.of(1990, 1, 1), "test1@example.com");
        user2 = TestUtils.createUser("Jane", "Smith", LocalDate.of(1995, 2, 2), "test2@example.com");

        cardInfo1 = TestUtils.createCardInfo("1111222233334444", "John Doe", LocalDate.of(2025, 1, 1), user1);
        cardInfo2 = TestUtils.createCardInfo("2222333344445555", "Jane Smith", LocalDate.of(2026, 2, 2), user2);

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
        cardInfo1.setUser(user1);
    }

    @Test
    public void updateCardInfo_shouldThrowException_whenCardInfoNumberAlreadyExists() {
        cardInfo1.setNumber("1111222233334444");
        cardInfo1.setHolder("John Doe");
        cardInfo1.setExpirationDate(LocalDate.of(2025, 1, 1));
        cardInfo1.setUser(user1);
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
