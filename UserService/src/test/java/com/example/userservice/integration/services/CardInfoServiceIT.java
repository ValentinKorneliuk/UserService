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
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;
import com.redis.testcontainers.RedisContainer;
import org.testcontainers.utility.DockerImageName;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.userservice.services.CardInfoService;
import com.example.userservice.dto.CardInfoRequestDTO;
import com.example.userservice.entities.CardInfo;
import com.example.userservice.entities.User;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import com.example.userservice.repositories.CardInfoRepository;
import com.example.userservice.repositories.UserRepository;

import org.springframework.cache.Cache;
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.mappers.UserMapper;
import com.example.userservice.exceptions.ConflictException;
import com.example.userservice.TestUtils;

@SpringBootTest
@Testcontainers
public class CardInfoServiceIT {

    @Container
    private static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:8.0.2"));

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

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

    User user1;
    CardInfo cardInfo1;
    CardInfo cardInfo2;

    @BeforeEach
    void setUp() {
        cardInfoRepository.deleteAll();
        userRepository.deleteAll();

        user1 = TestUtils.createUser("John", "Doe", LocalDate.of(1990, 1, 1), "test1@example.com");
        cardInfo1 = TestUtils.createCardInfo("1111222233334444", "John Doe", LocalDate.of(2027, 1, 1), user1);
        cardInfo2 = TestUtils.createCardInfo("2222333344445555", "Jane Doe", LocalDate.of(2027, 1, 1), user1);

        user1.setCardInfos(new ArrayList<>(List.of(cardInfo1, cardInfo2)));

        userRepository.saveAllAndFlush(List.of(user1));
    }

    @Test
    public void createCardInfo_shouldCreateCardInfoAndDeleteUserFromCache() {
        CardInfoRequestDTO cardInfoRequestDTO = TestUtils.createCardInfoRequestDTO(
            "3333444455556666", 
            "JOHN DOE", 
            LocalDate.of(2027, 1, 1), 
            user1.getId()
        );

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        cache.put(user1.getId(), userMapper.toDTO(user1));

        cardInfoService.createCardInfo(cardInfoRequestDTO);

        UserResponseDTO cachedUser = cache.get(user1.getId(), UserResponseDTO.class);
        assertNull(cachedUser);
    }

    @Test
    public void updateCardInfo_shouldRollbackTransactionOnConflict() {
        CardInfoRequestDTO cardInfoRequestDTO = TestUtils.createCardInfoRequestDTO(
            "2222333344445555", 
            "JOHN DOE", 
            LocalDate.of(2027, 1, 1), 
            user1.getId()
        );


        ConflictException conflictException = assertThrows(ConflictException.class, () -> {
            cardInfoService.updateCardInfo(cardInfo1.getId(), cardInfoRequestDTO);
        });
        assertEquals("Card info with number 2222333344445555 already exists", conflictException.getMessage());
    }

    @Test
    public void updateCardInfo_shouldUpdateCardInfoAndDeleteUserFromCache() {
        CardInfoRequestDTO cardInfoRequestDTO = TestUtils.createCardInfoRequestDTO(
            "3333444455556666", 
            "JOHN DOE", 
            LocalDate.of(2027, 1, 1), 
            user1.getId()
        );

        cardInfoService.updateCardInfo(cardInfo1.getId(), cardInfoRequestDTO);

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        UserResponseDTO cachedUser = cache.get(user1.getId(), UserResponseDTO.class);
        assertNull(cachedUser);
    }

    @Test
    public void deleteCardInfo_shouldDeleteCardInfoAndDeleteUserFromCache() {
        Long cardInfoId = cardInfo1.getId();

        cardInfoService.deleteCardInfo(cardInfoId);

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        UserResponseDTO cachedUser = cache.get(user1.getId(), UserResponseDTO.class);
        assertNull(cachedUser);
    }
}
