package com.beet.backend.modules.cash.application.dto;

import com.beet.backend.modules.cash.domain.model.CashMovementDirection;
import com.beet.backend.modules.cash.domain.model.CashMovementReason;
import com.beet.backend.modules.cash.domain.model.CashMovementStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CashMovementResponse(
        UUID id,
        UUID restaurantId,
        UUID businessDayId,
        UUID cashSessionId,
        CashMovementDirection direction,
        CashMovementReason reason,
        BigDecimal amount,
        CashMovementStatus status,
        String notes,
        OffsetDateTime createdAt,
        UUID createdBy,
        UUID createdDeviceId,
        OffsetDateTime voidedAt,
        UUID voidedBy,
        String voidReason) {
}
