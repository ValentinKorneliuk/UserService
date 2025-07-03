package com.example.userservice.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

public class UserResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String name;
    private String surname;
    private LocalDate birthDate;
    private String email;
    private List<CardInfoResponseDTO> cardInfos;


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<CardInfoResponseDTO> getCardInfos() { return cardInfos; }
    public void setCardInfos(List<CardInfoResponseDTO> cardInfos) { this.cardInfos = cardInfos; }
}
