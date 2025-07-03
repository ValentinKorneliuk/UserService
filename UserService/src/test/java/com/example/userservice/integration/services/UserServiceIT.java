package com.example.userservice.integration.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;
import com.redis.testcontainers.RedisContainer;
import org.testcontainers.utility.DockerImageName;
import com.example.userservice.dto.UserRequestDTO;
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.entities.CardInfo;
import com.example.userservice.entities.User;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.services.UserService;

import com.example.userservice.exceptions.ConflictException;
import com.example.userservice.TestUtils;

import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;


@SpringBootTest
@Testcontainers
public class UserServiceIT {

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
    private UserService userService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoSpyBean
    private UserRepository userRepository;

    User user1;
    User user2;
    CardInfo cardInfo1;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        user1 = TestUtils.createUser("John", "Doe", LocalDate.of(1990, 1, 1), "test1@example.com");
        user2 = TestUtils.createUser("Jane", "Doe", LocalDate.of(1990, 1, 1), "test2@example.com");

        cardInfo1 = TestUtils.createCardInfo("1111222233334444", "John Doe", LocalDate.of(2025, 1, 1), user1);

        user1.setCardInfos(new ArrayList<>(List.of(cardInfo1)));
        userRepository.saveAllAndFlush(List.of(user1, user2));
    }

    @Test
    public void getUserById_shouldUserCacheOnSecondCall() {
        Long userId = user1.getId();

        UserResponseDTO firstCall = userService.getUserById(userId);
        UserResponseDTO secondCall = userService.getUserById(userId);

        assertEquals(firstCall.getEmail(), secondCall.getEmail());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    public void getUserByEmail_shouldUserCacheOnSecondCall() {
        String userEmail = user1.getEmail();

        UserResponseDTO firstCall = userService.getUserByEmail(userEmail);
        UserResponseDTO secondCall = userService.getUserByEmail(userEmail);

        assertEquals(firstCall.getEmail(), secondCall.getEmail());
        verify(userRepository, times(1)).findByEmail(userEmail);
    }

    @Test
    public void updateUser_shouldRollbackTransactionOnConflict() {
        UserRequestDTO userRequestDTO = TestUtils.createUserRequestDTO(
            "Jane", 
            "Doe", LocalDate.of(1990, 1, 1), 
            "test2@example.com"
        );

        assertThrows(ConflictException.class, () -> {
            userService.updateUser(user1.getId(), userRequestDTO);
        });

        User unchangedUser = userRepository.findById(user1.getId()).orElseThrow();
        assertEquals(user1.getEmail(), unchangedUser.getEmail());
    }

    @Test
    public void updateUser_shouldPutUpdatedUserInCache() {
        UserRequestDTO userRequestDTO = TestUtils.createUserRequestDTO(
            "Jane", 
            "Doe", 
            LocalDate.of(1990, 1, 1), 
            "newemail@example.com"
        );

        userService.updateUser(user1.getId(), userRequestDTO);

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        UserResponseDTO cachedUser = cache.get(user1.getId(), UserResponseDTO.class);
        assertNotNull(cachedUser);
        assertEquals(userRequestDTO.getEmail(), cachedUser.getEmail());
    }

    @Test
    public void deleteUser_shouldDeleteUserFromCache() {
        Long userId = user1.getId();
        userService.deleteUser(userId);

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        UserResponseDTO cachedUser = cache.get(userId, UserResponseDTO.class);
        assertNull(cachedUser);
    }

}
