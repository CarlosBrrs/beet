package com.beet.backend.shared.infrastructure.input.rest;

import com.beet.backend.shared.domain.exception.ResourceAlreadyExistsException;
import com.beet.backend.shared.domain.exception.ResourceNotFoundException;
import com.beet.backend.shared.domain.exception.ResourceLimitExceededException;
import com.beet.backend.modules.ingredient.domain.exception.UnitTypeMismatchException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiGenericResponse<Void> handleResourceAlreadyExists(ResourceAlreadyExistsException ex) {
        String message = String.format("%s: %s", ex.getClass().getSimpleName(), ex.getMessage());
        return ApiGenericResponse.error(message);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiGenericResponse<Void> handleNotFound(ResourceNotFoundException ex) {
        String message = String.format("%s: %s", ex.getClass().getSimpleName(), ex.getMessage());
        return ApiGenericResponse.error(message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiGenericResponse<Void> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        String message = String.format("%s: %s", ex.getClass().getSimpleName(), errors.toString());
        return ApiGenericResponse.error(message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiGenericResponse<Void> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        return ApiGenericResponse.error("Database error: " + message);
    }

    @ExceptionHandler(ResourceLimitExceededException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN) // or 402 PAYMENT_REQUIRED if strictly about plan limits
    public ApiGenericResponse<Void> handleResourceLimitExceeded(ResourceLimitExceededException ex) {
        String message = String.format("%s: %s", ex.getClass().getSimpleName(), ex.getMessage());
        return ApiGenericResponse.error(message);
    }

    @ExceptionHandler(UnitTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiGenericResponse<Void> handleUnitTypeMismatch(UnitTypeMismatchException ex) {
        return ApiGenericResponse.error(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiGenericResponse<Void> handleAllExceptions(Exception ex) {
        String message = String.format("Unhandled exception [%s]: %s",
                ex.getClass().getName(),
                ex.getMessage());
        return ApiGenericResponse.error(message);
    }
}
