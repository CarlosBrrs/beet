package com.beet.backend.modules.cash.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BusinessDayClosureResponse(
        UUID id,
        UUID restaurantId,
        UUID businessDayId,
        int closureSequence,
        BigDecimal paymentsTotal,
        BigDecimal tipsTotal,
        BigDecimal refundsTotal,
        BigDecimal cashInTotal,
        BigDecimal cashOutTotal,
        BigDecimal expectedCashTotal,
        BigDecimal countedCashTotal,
        BigDecimal differenceTotal,
        String notes,
        OffsetDateTime closedAt,
        UUID closedBy,
        List<PaymentTotalResponse> paymentTotals) {
}
