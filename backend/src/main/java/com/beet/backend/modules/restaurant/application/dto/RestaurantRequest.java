package com.beet.backend.modules.restaurant.application.dto;

import com.beet.backend.modules.restaurant.domain.model.RestaurantSettings;
import jakarta.validation.constraints.NotBlank;

public record RestaurantRequest(
        @NotBlank(message = "Name is required") String name,
        String address,
        String email,
        String phoneNumber,
        String operationMode,
        Boolean isActive, // Optional, defaults to true in logic if null
        RestaurantSettings settings) {
}
