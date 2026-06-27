package com.beet.backend.modules.order.application.dto;

import com.beet.backend.modules.order.domain.model.PaymentMethodType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentMethodResponse(
        UUID id,
        UUID restaurantId,
        String code,
        String name,
        PaymentMethodType type,
        boolean isActive,
        boolean requiresReference,
        int sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
