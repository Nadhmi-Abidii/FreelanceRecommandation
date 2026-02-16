package com.towork.exception;

import com.towork.config.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<MessageResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        MessageResponse messageResponse = MessageResponse.error(ex.getMessage());
        return new ResponseEntity<>(messageResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<MessageResponse> handleBusinessException(BusinessException ex, WebRequest request) {
        MessageResponse messageResponse = MessageResponse.error(ex.getMessage());
        return new ResponseEntity<>(messageResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidMilestoneStatusException.class)
    public ResponseEntity<MessageResponse> handleInvalidStatusException(InvalidMilestoneStatusException ex) {
        MessageResponse messageResponse = MessageResponse.error(ex.getMessage());
        return new ResponseEntity<>(messageResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<MessageResponse> handleForbiddenActionException(ForbiddenActionException ex) {
        MessageResponse messageResponse = MessageResponse.error(ex.getMessage());
        return new ResponseEntity<>(messageResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MilestoneNotFoundException.class)
    public ResponseEntity<MessageResponse> handleMilestoneNotFound(MilestoneNotFoundException ex) {
        MessageResponse messageResponse = MessageResponse.error(ex.getMessage());
        return new ResponseEntity<>(messageResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MessageResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        MessageResponse messageResponse = MessageResponse.error("Invalid credentials");
        return new ResponseEntity<>(messageResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<MessageResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        MessageResponse messageResponse = MessageResponse.error("Access denied");
        return new ResponseEntity<>(messageResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<MessageResponse> handleConflictException(ConflictException ex) {
        MessageResponse messageResponse = MessageResponse.error(ex.getMessage());
        return new ResponseEntity<>(messageResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        MessageResponse messageResponse = MessageResponse.error("Validation failed", errors);
        return new ResponseEntity<>(messageResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleGlobalException(Exception ex, WebRequest request) {
        MessageResponse messageResponse = MessageResponse.error("An unexpected error occurred: " + ex.getMessage());
        return new ResponseEntity<>(messageResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
