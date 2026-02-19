package com.beet.backend.modules.role.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceNotFoundException;

import java.util.UUID;

public class RoleNotFoundException extends ResourceNotFoundException {
    public RoleNotFoundException(String message) {
        super(message);
    }

    public static RoleNotFoundException forUserInRestaurant(UUID userId, UUID restaurantId) {
        return new RoleNotFoundException(
                String.format("Role not found for user %s in restaurant %s", userId, restaurantId));
    }

    public static RoleNotFoundException forName(String roleName) {
        return new RoleNotFoundException(
                String.format("Role not found for name %s", roleName));
    }
}
