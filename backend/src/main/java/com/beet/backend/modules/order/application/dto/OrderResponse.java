package com.beet.backend.modules.order.application.dto;

import com.beet.backend.modules.order.domain.model.KitchenStatus;
import com.beet.backend.modules.order.domain.model.OrderStatus;
import com.beet.backend.modules.order.domain.model.PaymentStatus;
import com.beet.backend.modules.order.domain.model.ServiceType;
import com.beet.backend.modules.order.domain.model.DeliveryStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID restaurantId,
        UUID cashSessionId,
        LocalDate businessDate,
        Integer dailySequence,
        String orderNumber,
        String publicCode,
        String displayCode,
        OrderStatus orderStatus,
        KitchenStatus kitchenStatus,
        PaymentStatus paymentStatus,
        ServiceType serviceType,
        UUID tableId,
        String customerName,
        String customerPhone,
        DeliveryStatus deliveryStatus,
        BigDecimal subtotalGrossSnapshot,
        BigDecimal taxAmountSnapshot,
        BigDecimal totalGrossSnapshot,
        BigDecimal tipTotalSnapshot,
        BigDecimal refundDueSnapshot,
        BigDecimal refundedTotalSnapshot,
        BigDecimal paidTotal,
        BigDecimal remainingBalance,
        OffsetDateTime paymentExpiresAt,
        OffsetDateTime paymentExpiredAt,
        boolean paymentExpired,
        Integer prepaidOrderExpirationMinutes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
