package com.example.userservice.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CardInfoResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private UUID id;
    private String number;
    private String holder;
    private LocalDate expirationDate;
    private UUID userId;

}
