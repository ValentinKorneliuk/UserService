package com.example.userservice.exceptions;

public class ConflictException extends RuntimeException {
    public static final String CARD_INFO_ALREADY_EXISTS = "Card info with number %s already exists";
    public static final String USER_ALREADY_EXISTS = "User with email %s already exists";

    public ConflictException(String message) {
        super(message);
    }
}
