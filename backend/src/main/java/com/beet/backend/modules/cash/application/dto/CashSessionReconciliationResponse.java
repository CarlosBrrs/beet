package com.beet.backend.modules.cash.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CashSessionReconciliationResponse(
        UUID id,
        UUID restaurantId,
        UUID businessDayId,
        UUID cashSessionId,
        BigDecimal openingAmount,
        BigDecimal paymentsTotal,
        BigDecimal tipsTotal,
        BigDecimal refundsTotal,
        BigDecimal cashInTotal,
        BigDecimal cashOutTotal,
        BigDecimal expectedCash,
        BigDecimal countedCash,
        BigDecimal differenceAmount,
        String differenceReason,
        String notes,
        OffsetDateTime closedAt,
        UUID closedBy,
        UUID closedDeviceId,
        boolean blind,
        boolean closed,
        List<PaymentTotalResponse> paymentTotals) {
}
