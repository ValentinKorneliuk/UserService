package com.example.userservice.advices;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.userservice.dto.ErrorResponseDTO;
import com.example.userservice.exceptions.ConflictException;
import com.example.userservice.exceptions.ResourceNotFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: [{} {}] - {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Not found",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = getValidationExceptionErrorMessages(ex);
        log.warn("Validation failed: [{} {}] - {}", request.getMethod(), request.getRequestURI(), errors);
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "Validation errors",
                request.getRequestURI(),
                errors
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing request parameter: [{} {}] - {}", request.getMethod(), request.getRequestURI(), ex.getParameterName() + " parameter is missing");
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Missing request parameter",
                ex.getParameterName() + " parameter is missing",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingPathVariable(MissingPathVariableException ex, HttpServletRequest request) {
        log.warn("Missing path variable: [{} {}] - {}", request.getMethod(), request.getRequestURI(), ex.getVariableName() + " path variable is missing");
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Missing path variable",
                ex.getVariableName() + " path variable is missing",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation: [{} {}] - {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Data integrity violation",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponseDTO> handleConflict(ConflictException ex, HttpServletRequest request) {
        log.warn("Conflict error: [{} {}] - {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Conflict",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthorization(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: [{} {}] - {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleOtherErrors(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: [{} {}]", request.getMethod(), request.getRequestURI(), ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    private List<String> getValidationExceptionErrorMessages(MethodArgumentNotValidException ex) {
        return ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .toList();
    }

    private ResponseEntity<ErrorResponseDTO> buildErrorResponse(
            HttpStatus status,
            String error,
            String message,
            String path,
            List<String> messages
    ) {
        ErrorResponseDTO.ErrorResponseDTOBuilder builder = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(path);

        if (messages != null && !messages.isEmpty()) {
            builder.messages(messages);
        }

        return ResponseEntity.status(status).body(builder.build());
    }
}