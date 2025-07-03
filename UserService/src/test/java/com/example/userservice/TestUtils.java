package com.example.userservice;

import com.example.userservice.dto.UserRequestDTO;
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.dto.CardInfoRequestDTO;
import com.example.userservice.dto.CardInfoResponseDTO;
import com.example.userservice.entities.User;
import com.example.userservice.entities.CardInfo;
import java.time.LocalDate;

public class TestUtils {
    
    public static User createUser(String name, String surname, LocalDate birthDate, String email) {
        User user = new User();
        user.setName(name);
        user.setSurname(surname);
        user.setBirthDate(birthDate);
        user.setEmail(email);
        return user;
    }

    public static CardInfo createCardInfo(String number, String holder, LocalDate expirationDate, User user) {
        CardInfo cardInfo = new CardInfo();
        cardInfo.setNumber(number);
        cardInfo.setHolder(holder);
        cardInfo.setExpirationDate(expirationDate);
        cardInfo.setUser(user);
        return cardInfo;
    }

    public static UserRequestDTO createUserRequestDTO(String name, String surname, LocalDate birthDate, String email) {
        UserRequestDTO userRequestDTO = new UserRequestDTO();
        userRequestDTO.setName(name);
        userRequestDTO.setSurname(surname);
        userRequestDTO.setBirthDate(birthDate);
        userRequestDTO.setEmail(email);
        return userRequestDTO;
    }

    public static UserResponseDTO createUserResponseDTO(Long id, String name, String surname, LocalDate birthDate, String email) {
        UserResponseDTO userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(id);
        userResponseDTO.setName(name);
        userResponseDTO.setSurname(surname);
        userResponseDTO.setBirthDate(birthDate);
        userResponseDTO.setEmail(email);
        return userResponseDTO;
    }

    public static CardInfoRequestDTO createCardInfoRequestDTO(String number, String holder, LocalDate expirationDate, Long userId) {
        CardInfoRequestDTO cardInfoRequestDTO = new CardInfoRequestDTO();
        cardInfoRequestDTO.setNumber(number);
        cardInfoRequestDTO.setHolder(holder);
        cardInfoRequestDTO.setExpirationDate(expirationDate);
        cardInfoRequestDTO.setUserId(userId);
        return cardInfoRequestDTO;
    }

    public static CardInfoResponseDTO createCardInfoResponseDTO(Long id, String number, String holder, LocalDate expirationDate, Long userId) {
        CardInfoResponseDTO cardInfoResponseDTO = new CardInfoResponseDTO();
        cardInfoResponseDTO.setId(id);
        cardInfoResponseDTO.setNumber(number);
        cardInfoResponseDTO.setHolder(holder);
        cardInfoResponseDTO.setExpirationDate(expirationDate);
        cardInfoResponseDTO.setUserId(userId);
        return cardInfoResponseDTO;
    }
}
