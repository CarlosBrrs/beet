package com.beet.backend.shared.domain.exception;

/**
 * Thrown when an authenticated user does not have the required permission
 * to perform an action on a specific restaurant resource.
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public static AccessDeniedException forPermission(String module, String action, String restaurantId) {
        return new AccessDeniedException(
                String.format("User does not have permission %s:%s on restaurant %s", module, action, restaurantId));
    }
}
