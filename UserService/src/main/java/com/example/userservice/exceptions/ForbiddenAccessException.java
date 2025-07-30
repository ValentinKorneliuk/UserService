package com.example.userservice.exceptions;

import org.springframework.security.authorization.AuthorizationDeniedException;

public class ForbiddenAccessException extends AuthorizationDeniedException {
    public static final String ACCESS_FORBIDDEN = "Access to this resource is forbidden";

    public ForbiddenAccessException(String message) {
        super(message);
    }
}