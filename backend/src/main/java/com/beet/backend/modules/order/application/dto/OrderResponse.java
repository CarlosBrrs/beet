package com.beet.backend.modules.order.application.dto;

import com.beet.backend.modules.order.domain.model.KitchenStatus;
import com.beet.backend.modules.order.domain.model.OrderStatus;
import com.beet.backend.modules.order.domain.model.PaymentStatus;
import com.beet.backend.modules.order.domain.model.ServiceType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID restaurantId,
        UUID cashSessionId,
        OrderStatus orderStatus,
        KitchenStatus kitchenStatus,
        PaymentStatus paymentStatus,
        ServiceType serviceType,
        UUID tableId,
        String customerName,
        BigDecimal subtotalGrossSnapshot,
        BigDecimal taxAmountSnapshot,
        BigDecimal totalGrossSnapshot,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
