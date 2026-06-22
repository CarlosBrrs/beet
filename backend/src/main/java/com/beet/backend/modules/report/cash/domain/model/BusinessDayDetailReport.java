package com.beet.backend.modules.report.cash.domain.model;

import com.beet.backend.modules.report.core.domain.model.ReportResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BusinessDayDetailReport(
        UUID id,
        UUID restaurantId,
        String restaurantName,
        LocalDate businessDate,
        String timeZone,
        String status,
        boolean provisional,
        List<Event> events,
        List<Closure> closures,
        List<Session> sessions) implements ReportResult {

    public record Event(String type, String reason, OffsetDateTime occurredAt, UUID occurredBy) {
    }

    public record Closure(
            UUID id,
            int sequence,
            BigDecimal payments,
            BigDecimal tips,
            BigDecimal refunds,
            BigDecimal cashIn,
            BigDecimal cashOut,
            BigDecimal expectedCash,
            BigDecimal countedCash,
            BigDecimal difference,
            String notes,
            OffsetDateTime closedAt) {
    }

    public record Session(
            UUID id,
            String registerName,
            String status,
            BigDecimal openingAmount,
            BigDecimal expectedCash,
            BigDecimal countedCash,
            BigDecimal difference,
            String differenceReason,
            OffsetDateTime openedAt,
            OffsetDateTime closedAt) {
    }
}
