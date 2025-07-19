package com.example.userservice.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private UUID id;
    private String name;
    private String surname;
    private LocalDate birthDate;
    private String email;
    private List<CardInfoResponseDTO> cardInfos;

}
