package com.example.userservice.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    public static final String CARD_INFO_NOT_FOUND = "Card info not found";
    public static final String USER_NOT_FOUND = "User not found";

    public ResourceNotFoundException(String message) {
        super(message);
    }
}