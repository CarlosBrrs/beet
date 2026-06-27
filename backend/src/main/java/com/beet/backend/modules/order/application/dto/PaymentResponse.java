package com.beet.backend.modules.order.application.dto;

import com.beet.backend.modules.order.domain.model.PaymentRecordStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID restaurantId,
        UUID orderId,
        UUID orderBillId,
        UUID paymentMethodId,
        UUID cashSessionId,
        BigDecimal amount,
        BigDecimal tipAmount,
        PaymentRecordStatus status,
        String externalReference,
        String notes,
        OffsetDateTime createdAt) {
}
