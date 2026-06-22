package com.beet.backend.modules.cash.domain.model;

import com.beet.backend.modules.order.domain.model.PaymentMethodType;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentTotalSnapshot(
        UUID paymentMethodId,
        String methodCode,
        String methodName,
        PaymentMethodType methodType,
        int paymentCount,
        BigDecimal paymentAmount,
        BigDecimal tipAmount,
        BigDecimal refundAmount,
        BigDecimal netAmount) {
}
