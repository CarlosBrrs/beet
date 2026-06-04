package com.beet.backend.modules.table.application.dto;

import com.beet.backend.modules.table.domain.model.TableAvailabilityStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RestaurantTableResponse(
        UUID id,
        UUID restaurantId,
        String name,
        Integer capacity,
        String area,
        Integer sortOrder,
        boolean isActive,
        String notes,
        TableAvailabilityStatus availabilityStatus,
        UUID openOrderId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
