package com.beet.backend.modules.restaurant.application.dto;

import com.beet.backend.modules.restaurant.domain.model.RestaurantSettings;
import lombok.Builder;

import java.util.UUID;

@Builder
public record RestaurantResponse(
                UUID id,
                String name,
                String address,
                String email,
                String phoneNumber,
                String operationMode,
                Boolean isActive,
                UUID ownerId,
                RestaurantSettings settings,
                String role) {
}
