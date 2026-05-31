package com.beet.backend.modules.cash.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CashSessionSummary(
        UUID id,
        UUID restaurantId,
        String restaurantName,
        UUID cashRegisterId,
        String cashRegisterName,
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
