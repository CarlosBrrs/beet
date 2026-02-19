package com.beet.backend.modules.restaurant.domain.exception;

public class RoleAssignmentException extends RuntimeException {

    public RoleAssignmentException(String message, Throwable cause) {
        super(message, cause);
    }

    public static RoleAssignmentException forRole(String roleName, Throwable cause) {
        return new RoleAssignmentException(String.format("Failed to assign '%s' role", roleName), cause);
    }
}
