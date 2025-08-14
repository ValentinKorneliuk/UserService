package com.example.userservice.integration.repositories;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.userservice.integration.BaseContainerIntegrationTest;
import com.example.userservice.entities.User;
import com.example.userservice.entities.CardInfo;
import com.example.userservice.repositories.UserRepository;


class UserRepositoryIntegrationTest extends BaseContainerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

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

        user1.setCardInfos(new ArrayList<>(List.of(cardInfo1)));
        user2.setCardInfos(new ArrayList<>(List.of(cardInfo2)));
        userRepository.saveAll(List.of(user1, user2));
    }

    @Test
    void saveUser_shouldSaveUser_whenUserEmailUnique() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test3@example.com");
        user.setName("Anna  ");
        user.setSurname("Smith");
        user.setBirthDate(LocalDate.of(1992, 3, 20));

        UUID savedUserId = userRepository.save(user).getId();

        Optional<User> retrievedUser = userRepository.findById(savedUserId);
        assertNotNull(retrievedUser);
        assertEquals(user.getEmail(), retrievedUser.get().getEmail());
        assertEquals(user.getName(), retrievedUser.get().getName());
        assertEquals(user.getSurname(), retrievedUser.get().getSurname());
        assertEquals(user.getBirthDate(), retrievedUser.get().getBirthDate());
    }

    @Test
    void saveUser_shouldThrowException_whenEmailAlreadyExists() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test1@example.com");
        user.setName("Jane");
        user.setSurname("Smith");
        user.setBirthDate(LocalDate.of(1995, 2, 15));

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(user));
    }

    @Test
    void updateUser_shouldUpdateUser_whenUserEmailUnique() {
        user1.setEmail("test4@example.com");
        user1.setName("Bob");
        user1.setSurname("Michaels");
        user1.setBirthDate(LocalDate.of(1993, 4, 25));
        user1.setCardInfos(new ArrayList<>());

        userRepository.saveAndFlush(user1);

        Optional<User> retrievedUser = userRepository.findById(user1.getId());
        assertNotNull(retrievedUser);
        assertEquals(user1.getEmail(), retrievedUser.get().getEmail());
        assertEquals(user1.getName(), retrievedUser.get().getName());
        assertEquals(user1.getSurname(), retrievedUser.get().getSurname());
        assertEquals(user1.getBirthDate(), retrievedUser.get().getBirthDate());
    }

    @Test
    void updateUser_shouldThrowException_whenEmailAlreadyExists() {
        user1.setEmail("test2@example.com");
        user1.setName("Bob");
        user1.setSurname("Michaels");
        user1.setBirthDate(LocalDate.of(1993, 4, 25));

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(user1));
    }

    @Test
    void deleteUser_shouldDeleteUser_whenUserExists() {
        UUID userId = user1.getId();
        userRepository.deleteById(userId);
        Optional<User> retrievedUser = userRepository.findById(userId);
        assertEquals(true, retrievedUser.isEmpty());
    }

    @Test
    void findById_shouldReturnUserWithCardInfos_whenUserExists() {
        UUID userId = user1.getId();

        Optional<User> retrievedUser = userRepository.findById(userId);

        assertNotNull(retrievedUser);
        assertEquals(user1.getEmail(), retrievedUser.get().getEmail());
        assertEquals(1, retrievedUser.get().getCardInfos().size());
    }

    @Test
    void findById_shouldReturnNull_whenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        Optional<User> retrievedUser = userRepository.findById(userId);
        assertEquals(true, retrievedUser.isEmpty());
    }

    @Test
    void findByEmail_shouldReturnUserWithCardInfos_whenUserExists() {
        String email = user1.getEmail();

        Optional<User> retrievedUser = userRepository.findByEmail(email);

        assertNotNull(retrievedUser);
        assertEquals(email, retrievedUser.get().getEmail());
        assertEquals(1, retrievedUser.get().getCardInfos().size());
    }

    @Test
    void findByEmail_shouldReturnNull_whenUserDoesNotExist() {
        String email = "test3@example.com";
        Optional<User> retrievedUser = userRepository.findByEmail(email);
        assertEquals(true, retrievedUser.isEmpty());
    }

    @Test
    void findUsersByIds_shouldReturnUsersWithCardInfos_whenUsersExist() {
        List<UUID> userIds = List.of(user1.getId(), user2.getId());

        List<User> retrievedUsers = userRepository.findUsersByIds(userIds);

        assertNotNull(retrievedUsers);
        assertEquals(2, retrievedUsers.size());

        List<String> emails = retrievedUsers.stream().map(User::getEmail).toList();
        assertTrue(emails.contains(user1.getEmail()));
        assertTrue(emails.contains(user2.getEmail()));

        retrievedUsers.forEach(user -> {
            assertNotNull(user.getCardInfos());
            assertEquals(1, user.getCardInfos().size());
        });
    }

    @Test
    void existsByEmail_shouldReturnTrue_whenUserExists() {
        String email = user1.getEmail();
        boolean exists = userRepository.existsByEmail(email);
        assertEquals(true, exists);
    }

    @Test
    void existsByEmail_shouldReturnFalse_whenUserDoesNotExist() {
        String email = "test3@example.com";
        boolean exists = userRepository.existsByEmail(email);
        assertEquals(false, exists);
    }
}