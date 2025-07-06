package com.example.userservice.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class CardInfoRequestDTO {

    @NotBlank(message = "Card number is required")
    private String number;

    @NotBlank(message = "Card holder is required")
    private String holder;

    @NotNull(message = "Expiration date is required")
    @Future(message = "Expiration date must be in the future")
    private LocalDate expirationDate;

    @NotNull(message = "User ID is required")
    private Long userId;

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public String getHolder() { return holder; }
    public void setHolder(String holder) { this.holder = holder; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
