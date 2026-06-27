package com.beet.backend.modules.report.cash.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BusinessDayReportRow(
        UUID id,
        UUID restaurantId,
        String restaurantName,
        LocalDate businessDate,
        String timeZone,
        String status,
        boolean provisional,
        int closureSequence,
        BigDecimal paymentsTotal,
        BigDecimal tipsTotal,
        BigDecimal refundsTotal,
        BigDecimal expectedCash,
        BigDecimal countedCash,
        BigDecimal difference,
        OffsetDateTime openedAt,
        OffsetDateTime closedAt) {
}
