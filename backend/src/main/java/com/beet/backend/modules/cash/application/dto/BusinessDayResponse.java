package com.beet.backend.modules.cash.application.dto;

import com.beet.backend.modules.cash.domain.model.BusinessDayStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BusinessDayResponse(
        UUID id,
        UUID restaurantId,
        LocalDate businessDate,
        String timeZone,
        BusinessDayStatus status,
        OffsetDateTime openedAt,
        UUID openedBy,
        OffsetDateTime closedAt,
        UUID closedBy,
        int openSessionCount,
        int pendingOrderCount,
        int missingReconciliationCount,
        int unexplainedDifferenceCount) {
}
