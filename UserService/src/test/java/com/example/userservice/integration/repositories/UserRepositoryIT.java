package com.example.userservice.integration.repositories;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.userservice.entities.User;
import com.example.userservice.entities.CardInfo;   
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.TestUtils;

@DataJpaTest
@Testcontainers
public class UserRepositoryIT {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
        .withDatabaseName("test_db")
        .withUsername("test_user")
        .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

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

        user2 = TestUtils.createUser("Jane", "Smith", LocalDate.of(1995, 2, 2), "test2@example.com");

        cardInfo1 = TestUtils.createCardInfo("1111222233334444", "John Doe", LocalDate.of(2025, 1, 1), user1);
        cardInfo2 = TestUtils.createCardInfo("2222333344445555", "Jane Smith", LocalDate.of(2026, 2, 2), user2);

        user1.setCardInfos(new ArrayList<>(List.of(cardInfo1)));
        user2.setCardInfos(new ArrayList<>(List.of(cardInfo2)));
        userRepository.saveAll(List.of(user1, user2));
    }

    @Test
    public void saveUser_shouldSaveUser_whenUserEmailUnique() {
        User user = new User();
        user.setEmail("test3@example.com");
        user.setName("Anna  ");
        user.setSurname("Smith");
        user.setBirthDate(LocalDate.of(1992, 3, 20));

        Long savedUserId = userRepository.save(user).getId();

        Optional<User> retrievedUser = userRepository.findById(savedUserId);
        assertNotNull(retrievedUser);
        assertEquals(user.getEmail(), retrievedUser.get().getEmail());
        assertEquals(user.getName(), retrievedUser.get().getName());
        assertEquals(user.getSurname(), retrievedUser.get().getSurname());
        assertEquals(user.getBirthDate(), retrievedUser.get().getBirthDate());
    }

    @Test
    public void saveUser_shouldThrowException_whenEmailAlreadyExists() {
        User user = new User();
        user.setEmail("test1@example.com");
        user.setName("Jane");
        user.setSurname("Smith");
        user.setBirthDate(LocalDate.of(1995, 2, 15));

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.save(user));   
    }

    @Test
    public void updateUser_shouldUpdateUser_whenUserEmailUnique() {
        user1.setEmail("test4@example.com");
        user1.setName("Bob");
        user1.setSurname("Michaels");
        user1.setBirthDate(LocalDate.of(1993, 4, 25));

        userRepository.save(user1);

        Optional<User> retrievedUser = userRepository.findById(user1.getId());
        assertNotNull(retrievedUser);
        assertEquals(user1.getEmail(), retrievedUser.get().getEmail());
        assertEquals(user1.getName(), retrievedUser.get().getName());
        assertEquals(user1.getSurname(), retrievedUser.get().getSurname());
        assertEquals(user1.getBirthDate(), retrievedUser.get().getBirthDate());
    }

    @Test
    public void updateUser_shouldThrowException_whenEmailAlreadyExists() {
        user1.setEmail("test2@example.com");
        user1.setName("Bob");
        user1.setSurname("Michaels");
        user1.setBirthDate(LocalDate.of(1993, 4, 25)); 

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(user1));
    }

    @Test
    public void deleteUser_shouldDeleteUser_whenUserExists() {
        Long userId = user1.getId();
        userRepository.deleteById(userId);
        Optional<User> retrievedUser = userRepository.findById(userId);
        assertEquals(retrievedUser.isEmpty(), true);
    }

    @Test
    public void findById_shouldReturnUserWithCardInfos_whenUserExists() {
        Long userId = user1.getId();
        
        Optional<User> retrievedUser = userRepository.findById(userId);

        assertNotNull(retrievedUser);
        assertEquals(retrievedUser.get().getEmail(), user1.getEmail());
        assertEquals(retrievedUser.get().getCardInfos().size(), 1);
    }

    @Test
    public void findById_shouldReturnNull_whenUserDoesNotExist() {
        Long userId = 100L;
        Optional<User> retrievedUser = userRepository.findById(userId);
        assertEquals(retrievedUser.isEmpty(), true);
    }

    @Test
    public void findByEmail_shouldReturnUserWithCardInfos_whenUserExists() {
        String email = user1.getEmail();
        
        Optional<User> retrievedUser = userRepository.findByEmail(email);

        assertNotNull(retrievedUser);
        assertEquals(retrievedUser.get().getEmail(), email);
        assertEquals(retrievedUser.get().getCardInfos().size(), 1);
    }

    @Test
    public void findByEmail_shouldReturnNull_whenUserDoesNotExist() {
        String email = "test3@example.com";
        Optional<User> retrievedUser = userRepository.findByEmail(email);
        assertEquals(retrievedUser.isEmpty(), true);
    }

    @Test
    public void findUsersByIds_shouldReturnUsersWithCardInfos_whenUsersExist() {
        List<Long> userIds = List.of(user1.getId(), user2.getId());

        List<User> retrievedUsers = userRepository.findUsersByIds(userIds);

        assertNotNull(retrievedUsers);
        assertEquals(retrievedUsers.size(), 2);

        List<String> emails = retrievedUsers.stream().map(User::getEmail).collect(Collectors.toList());
        assertTrue(emails.contains(user1.getEmail()));
        assertTrue(emails.contains(user2.getEmail()));

        retrievedUsers.forEach(user -> {
            assertNotNull(user.getCardInfos());
            assertEquals(user.getCardInfos().size(), 1);
        });
    }

    @Test
    public void existsByEmail_shouldReturnTrue_whenUserExists() {
        String email = user1.getEmail();
        boolean exists = userRepository.existsByEmail(email);
        assertEquals(exists, true);
    }

    @Test
    public void existsByEmail_shouldReturnFalse_whenUserDoesNotExist() {
        String email = "test3@example.com";
        boolean exists = userRepository.existsByEmail(email);
        assertEquals(exists, false);
    }
}
