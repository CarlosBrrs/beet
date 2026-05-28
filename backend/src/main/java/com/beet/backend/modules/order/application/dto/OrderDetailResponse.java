package com.beet.backend.modules.order.application.dto;

import com.beet.backend.modules.order.domain.model.KitchenStatus;
import com.beet.backend.modules.order.domain.model.OrderStatus;
import com.beet.backend.modules.order.domain.model.PaymentStatus;
import com.beet.backend.modules.order.domain.model.ServiceType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
        UUID id,
        UUID restaurantId,
        OrderStatus orderStatus,
        KitchenStatus kitchenStatus,
        PaymentStatus paymentStatus,
        ServiceType serviceType,
        UUID tableId,
        String customerName,
        boolean prepaymentRequiredSnapshot,
        BigDecimal taxRateSnapshot,
        BigDecimal subtotalGrossSnapshot,
        BigDecimal taxAmountSnapshot,
        BigDecimal totalGrossSnapshot,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<OrderItemResponse> items,
        List<OrderTaxResponse> taxes) {

    public record OrderItemResponse(
            UUID id,
            UUID itemId,
            UUID submenuNodeId,
            String itemNameSnapshot,
            BigDecimal unitPriceSnapshot,
            BigDecimal theoreticalCostSnapshot,
            BigDecimal quantity,
            BigDecimal subtotalGrossSnapshot,
            List<OrderItemTaxResponse> taxes) {
    }

    public record OrderTaxResponse(
            UUID id,
            UUID taxId,
            String taxNameSnapshot,
            BigDecimal taxRateSnapshot,
            BigDecimal taxBaseSnapshot,
            BigDecimal taxAmountSnapshot) {
    }

    public record OrderItemTaxResponse(
            UUID id,
            UUID orderItemId,
            UUID taxId,
            String taxNameSnapshot,
            BigDecimal taxRateSnapshot,
            BigDecimal taxBaseSnapshot,
            BigDecimal taxAmountSnapshot) {
    }
}
