package com.beet.backend.modules.restaurant.domain.model;

import com.beet.backend.shared.domain.model.OperationMode;

import java.util.UUID;

public record RestaurantWithRole(
        UUID id,
        String name,
        OperationMode operationMode,
        Boolean isActive,
        UUID ownerId,
        RestaurantSettings settings,
        String roleName) {
}
