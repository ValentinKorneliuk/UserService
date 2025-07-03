package com.example.userservice.dto;

import java.io.Serializable;
import java.time.LocalDate;

public class CardInfoResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String number;
    private String holder;
    private LocalDate expirationDate;
    private Long userId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public String getHolder() { return holder; }
    public void setHolder(String holder) { this.holder = holder; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
