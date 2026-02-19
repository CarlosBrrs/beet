package com.beet.backend.modules.restaurant.application.dto;

import com.beet.backend.modules.restaurant.domain.model.RestaurantSettings;

public record RestaurantUpdateRequest(
        String name,
        String address,
        String email,
        String phoneNumber,
        String operationMode,
        Boolean isActive,
        RestaurantSettings settings) {
}
