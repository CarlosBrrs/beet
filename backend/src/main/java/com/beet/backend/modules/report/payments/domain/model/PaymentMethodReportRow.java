package com.beet.backend.modules.report.payments.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentMethodReportRow(
        UUID restaurantId,
        String restaurantName,
        UUID paymentMethodId,
        String methodCode,
        String methodName,
        String methodType,
        long paymentCount,
        BigDecimal collected,
        BigDecimal tips,
        BigDecimal refunds,
        BigDecimal netCollected) {
}
