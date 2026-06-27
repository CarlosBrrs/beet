package com.beet.backend.modules.cash.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CashRegisterResponse(
        UUID id,
        UUID restaurantId,
        String name,
        UUID deviceId,
        boolean isActive,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
