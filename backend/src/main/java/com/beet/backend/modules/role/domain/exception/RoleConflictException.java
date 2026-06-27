package com.beet.backend.modules.role.domain.exception;

/**
 * Thrown when a user has conflicting role types (e.g. both OWNER and an
 * operative role like MANAGER).
 * An owner should only have OWNER role assignments. An employee should never
 * have the OWNER role.
 */
public class RoleConflictException extends RuntimeException {

    public RoleConflictException(String message) {
        super(message);
    }

    public static RoleConflictException ownerWithOperativeRoles() {
        return new RoleConflictException(
                "User has conflicting role assignments: OWNER role cannot coexist with operative roles (MANAGER, CASHIER, etc.)");
    }
}
