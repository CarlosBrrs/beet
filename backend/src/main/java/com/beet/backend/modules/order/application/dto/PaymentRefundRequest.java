package com.beet.backend.modules.order.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRefundRequest(
        UUID paymentId,
        BigDecimal amount,
        String reason,
        String reference) {
}
