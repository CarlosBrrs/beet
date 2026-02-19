package com.beet.backend.shared.infrastructure.input.rest;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApiGenericResponse<T> {
    private boolean success;
    private T data;
    private LocalDateTime timestamp;
    private String errorMessage;

    public static <T> ApiGenericResponse<T> success(T data) {
        return ApiGenericResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiGenericResponse<T> error(String errorMessage) {
        return ApiGenericResponse.<T>builder()
                .success(false)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiGenericResponse<T> empty() {
        return ApiGenericResponse.<T>builder()
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
