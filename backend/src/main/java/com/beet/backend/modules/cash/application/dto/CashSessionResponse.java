package com.beet.backend.modules.cash.application.dto;

import com.beet.backend.modules.cash.domain.model.CashSessionStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CashSessionResponse(
        UUID id,
        UUID restaurantId,
        UUID cashRegisterId,
        CashSessionStatus status,
        OffsetDateTime openedAt,
        UUID openedBy,
        UUID openedDeviceId,
        BigDecimal openingAmount,
        OffsetDateTime closedAt,
        UUID closedBy,
        UUID closedDeviceId,
        BigDecimal closingAmount,
        String notes) {
}
