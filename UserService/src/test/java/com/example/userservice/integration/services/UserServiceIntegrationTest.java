package com.example.userservice.integration.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.userservice.dto.UpdateUserRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.entities.CardInfo;
import com.example.userservice.entities.User;
import com.example.userservice.integration.BaseContainerIntegrationTest;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.services.UserService;

import com.example.userservice.exceptions.ConflictException;

import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;


@ActiveProfiles("test")
@Sql(statements = {
        "TRUNCATE TABLE card_info RESTART IDENTITY CASCADE",
        "TRUNCATE TABLE users RESTART IDENTITY CASCADE"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SpringBootTest
class UserServiceIntegrationTest extends BaseContainerIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoSpyBean
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

    @BeforeEach
    void setUp() {

        user1 = User.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .name("John")
                .surname("Doe")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("test@gmail.com")
                .build();
        user2 = User.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .name("Jane")
                .surname("Doe")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("test2@gmail.com")
                .build();
        cardInfo1 = CardInfo.builder()
                .number("1111222233334444")
                .holder("John Doe")
                .expirationDate(LocalDate.of(2025, 1, 1))
                .user(user1)
                .build();

        user1.setCardInfos(new ArrayList<>(List.of(cardInfo1)));
        userRepository.saveAllAndFlush(List.of(user1, user2));
    }

    @Test
    void getUserById_shouldUserCacheOnSecondCall() {
        UUID userId = user1.getId();

        UserResponseDTO firstCall = userService.getUserById(userId);
        UserResponseDTO secondCall = userService.getUserById(userId);

        assertEquals(firstCall.getEmail(), secondCall.getEmail());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getUserByEmail_shouldUserCacheOnSecondCall() {
        String userEmail = user1.getEmail();

        UserResponseDTO firstCall = userService.getUserByEmail(userEmail);
        UserResponseDTO secondCall = userService.getUserByEmail(userEmail);

        assertEquals(firstCall.getEmail(), secondCall.getEmail());
        verify(userRepository, times(1)).findByEmail(userEmail);
    }

    @Test
    void updateUser_shouldRollbackTransactionOnConflict() {
        UpdateUserRequestDTO updateUserRequestDTO = UpdateUserRequestDTO.builder()
                .name("Jane")
                .surname("Doe")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("test2@gmail.com")
                .build();

        UUID userId = user1.getId();
        assertThrows(ConflictException.class, () -> userService.updateUser(userId, updateUserRequestDTO));

        User unchangedUser = userRepository.findById(user1.getId()).orElseThrow();
        assertEquals(user1.getEmail(), unchangedUser.getEmail());
    }

    @Test
    void updateUser_shouldPutUpdatedUserInCache() {
        UpdateUserRequestDTO updateUserRequestDTO = UpdateUserRequestDTO.builder()
                .name("Jane")
                .surname("Doe")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("newemail@example.com")
                .build();

        userService.updateUser(user1.getId(), updateUserRequestDTO);

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        UserResponseDTO cachedUser = cache.get(user1.getId(), UserResponseDTO.class);
        assertNotNull(cachedUser);
        assertEquals(updateUserRequestDTO.getEmail(), cachedUser.getEmail());
    }

    @Test
    void deleteUser_shouldDeleteUserFromCache() {
        UUID userId = user1.getId();
        userService.deleteUser(userId);

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        UserResponseDTO cachedUser = cache.get(userId, UserResponseDTO.class);
        assertNull(cachedUser);
    }

}