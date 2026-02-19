package com.beet.backend.shared.domain.exception;

public class ResourceLimitExceededException extends RuntimeException {
    public ResourceLimitExceededException(String message) {
        super(message);
    }
}
