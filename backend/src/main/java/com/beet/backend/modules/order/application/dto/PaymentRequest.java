package com.beet.backend.modules.order.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        UUID paymentMethodId,
        UUID orderBillId,
        BigDecimal amount,
        BigDecimal tipAmount,
        String externalReference,
        String notes) {
}
