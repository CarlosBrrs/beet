package com.beet.backend.modules.order.application.dto;

import com.beet.backend.modules.order.domain.model.PaymentRefundStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentRefundResponse(
        UUID id,
        UUID restaurantId,
        UUID orderId,
        UUID paymentId,
        UUID cashSessionId,
        BigDecimal amount,
        PaymentRefundStatus status,
        String reason,
        String externalReference,
        OffsetDateTime createdAt) {
}
