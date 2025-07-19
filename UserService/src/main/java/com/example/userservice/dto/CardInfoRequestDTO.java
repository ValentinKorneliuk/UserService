package com.example.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CardInfoRequestDTO {

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^(\\d{4}[- ]?){3}\\d{4}$", message = "Card number must be 16 digits, optionally separated by spaces or hyphens")
    private String number;

    @NotBlank(message = "Card holder is required")
    private String holder;

    @NotNull(message = "Expiration date is required")
    @Future(message = "Expiration date must be in the future")
    private LocalDate expirationDate;

    @NotNull(message = "User ID is required")
    private UUID userId;
}
