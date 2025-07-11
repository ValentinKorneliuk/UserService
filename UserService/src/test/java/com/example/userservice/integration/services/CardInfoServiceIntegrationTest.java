package com.example.userservice.integration.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.userservice.services.CardInfoService;
import com.example.userservice.dto.CardInfoRequestDTO;
import com.example.userservice.entities.CardInfo;
import com.example.userservice.entities.User;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

import com.example.userservice.repositories.CardInfoRepository;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.integration.BaseContainerIntegrationTest;

import org.springframework.cache.Cache;
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.mappers.UserMapper;
import com.example.userservice.exceptions.ConflictException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.example.userservice.exceptions.ConflictException.CARD_INFO_ALREADY_EXISTS;


@Sql(statements = {
    "TRUNCATE TABLE card_info RESTART IDENTITY CASCADE",
    "TRUNCATE TABLE users RESTART IDENTITY CASCADE"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SpringBootTest
class CardInfoServiceIntegrationTest extends BaseContainerIntegrationTest {

    @Autowired
    private CardInfoService cardInfoService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CacheManager cacheManager;

    @MockitoSpyBean
    private CardInfoRepository cardInfoRepository;

    @MockitoSpyBean
    private UserRepository userRepository;

    @DynamicPropertySource    static void configureProperties(DynamicPropertyRegistry registry) {
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
            .expirationDate(LocalDate.of(2027, 1, 1))
            .user(user1)
            .build();
        cardInfo2 = CardInfo.builder()
            .number("2222333344445555")
            .holder("Jane Doe")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .user(user1)
            .build();

        user1.setCardInfos(new ArrayList<>(List.of(cardInfo1, cardInfo2)));

        userRepository.saveAllAndFlush(List.of(user1));
    }

    @Test
    void createCardInfo_shouldCreateCardInfoAndDeleteUserFromCache() {
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("3333444455556666")
            .holder("JOHN DOE")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .userId(user1.getId())
            .build();

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        cache.put(user1.getId(), userMapper.toDTO(user1));

        cardInfoService.createCardInfo(cardInfoRequestDTO);

        UserResponseDTO cachedUser = cache.get(user1.getId(), UserResponseDTO.class);
        assertNull(cachedUser);
    }

    @Test
    void updateCardInfo_shouldRollbackTransactionOnConflict() {
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("2222333344445555")
            .holder("JOHN DOE")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .userId(user1.getId())
            .build();


        Long cardInfoId = cardInfo1.getId();
        ConflictException conflictException = assertThrows(
            ConflictException.class, 
            () -> cardInfoService.updateCardInfo(cardInfoId, cardInfoRequestDTO)
        );
        assertEquals(String.format(CARD_INFO_ALREADY_EXISTS, cardInfoRequestDTO.getNumber()), conflictException.getMessage());
    }

    @Test
    void updateCardInfo_shouldUpdateCardInfoAndDeleteUserFromCache() {
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("3333444455556666")
            .holder("JOHN DOE")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .userId(user1.getId())
            .build();

        cardInfoService.updateCardInfo(cardInfo1.getId(), cardInfoRequestDTO);

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        UserResponseDTO cachedUser = cache.get(user1.getId(), UserResponseDTO.class);
        assertNull(cachedUser);
    }

    @Test
    void deleteCardInfo_shouldDeleteCardInfoAndDeleteUserFromCache() {
        Long cardInfoId = cardInfo1.getId();

        cardInfoService.deleteCardInfo(cardInfoId);

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        UserResponseDTO cachedUser = cache.get(user1.getId(), UserResponseDTO.class);
        assertNull(cachedUser);
    }
}
