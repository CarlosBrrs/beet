package com.beet.backend.modules.restaurant.domain.model;

import com.beet.backend.shared.domain.model.OperationMode;

import java.util.UUID;

public record RestaurantWithRole(
        UUID id,
        String name,
        String address,
        String email,
        String phoneNumber,
        OperationMode operationMode,
        Boolean isActive,
        UUID ownerId,
        RestaurantSettings settings,
        String roleName) {

    public RestaurantWithRole(
            UUID id,
            String name,
            OperationMode operationMode,
            Boolean isActive,
            UUID ownerId,
            RestaurantSettings settings,
            String roleName) {
        this(id, name, null, null, null, operationMode, isActive, ownerId, settings, roleName);
    }
}
