package com.beet.backend.modules.order.application.dto;

import com.beet.backend.modules.order.domain.model.DeliveryStatus;
import com.beet.backend.modules.order.domain.model.KitchenStatus;
import com.beet.backend.modules.order.domain.model.KitchenTicketStatus;
import com.beet.backend.modules.order.domain.model.OrderLineType;
import com.beet.backend.modules.order.domain.model.OrderItemInventoryDisposition;
import com.beet.backend.modules.order.domain.model.OrderStatus;
import com.beet.backend.modules.order.domain.model.PaymentRecordStatus;
import com.beet.backend.modules.order.domain.model.PaymentRefundStatus;
import com.beet.backend.modules.order.domain.model.PaymentStatus;
import com.beet.backend.modules.order.domain.model.ServiceType;
import com.beet.backend.shared.domain.model.OperationMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
        UUID id,
        UUID restaurantId,
        UUID cashSessionId,
        UUID originCashSessionId,
        UUID originDeviceId,
        LocalDate businessDate,
        Integer dailySequence,
        String orderNumber,
        String publicCode,
        String displayCode,
        OrderStatus orderStatus,
        KitchenStatus kitchenStatus,
        PaymentStatus paymentStatus,
        ServiceType serviceType,
        OperationMode operationModeSnapshot,
        UUID tableId,
        String customerName,
        String customerPhone,
        String deliveryContactName,
        String deliveryPhone,
        String deliveryAddress,
        String deliveryNotes,
        BigDecimal deliveryFee,
        DeliveryStatus deliveryStatus,
        boolean prepaymentRequiredSnapshot,
        BigDecimal taxRateSnapshot,
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
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<OrderItemResponse> items,
        List<OrderTaxResponse> taxes,
        List<KitchenTicketResponse> kitchenTickets,
        List<PaymentResponse> payments,
        List<RefundResponse> refunds) {

    public record OrderItemResponse(
            UUID id,
            OrderLineType lineType,
            UUID itemId,
            UUID templateId,
            UUID submenuNodeId,
            String itemNameSnapshot,
            BigDecimal unitPriceSnapshot,
            BigDecimal theoreticalCostSnapshot,
            BigDecimal quantity,
            BigDecimal canceledQuantity,
            BigDecimal activeQuantity,
            BigDecimal subtotalGrossSnapshot,
            String notes,
            List<TemplateSlotSnapshotResponse> templateSlots,
            List<OrderItemTaxResponse> taxes,
            List<ItemCancellationResponse> cancellations) {
    }

    public record ItemCancellationResponse(
            UUID id,
            BigDecimal quantity,
            BigDecimal grossAmount,
            String reason,
            KitchenStatus kitchenStatusSnapshot,
            OrderItemInventoryDisposition inventoryDisposition,
            OffsetDateTime createdAt,
            UUID createdBy,
            boolean systemGenerated) {
    }

    public record TemplateSlotSnapshotResponse(
            UUID id,
            UUID templateSlotId,
            String slotNameSnapshot,
            int minSelectionSnapshot,
            int maxSelectionSnapshot,
            int sortOrder,
            List<TemplateOptionSnapshotResponse> options) {
    }

    public record TemplateOptionSnapshotResponse(
            UUID id,
            UUID slotOptionId,
            UUID itemId,
            String itemNameSnapshot,
            BigDecimal quantity,
            BigDecimal surchargeSnapshot,
            BigDecimal theoreticalCostSnapshot) {
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

    public record KitchenTicketResponse(
            UUID id,
            UUID orderId,
            String orderNumber,
            String orderPublicCode,
            String orderDisplayCode,
            String customerName,
            KitchenTicketStatus status,
            OffsetDateTime sentAt,
            OffsetDateTime startedAt,
            OffsetDateTime readyAt,
            OffsetDateTime canceledAt,
            List<KitchenTicketLineResponse> lines) {
    }

    public record KitchenTicketLineResponse(
            UUID id,
            UUID orderItemId,
            OrderLineType lineType,
            BigDecimal quantity,
            BigDecimal canceledQuantity,
            BigDecimal activeQuantity,
            String itemNameSnapshot,
            String notes,
            List<TemplateSlotSnapshotResponse> templateSlots) {
    }

    public record PaymentResponse(
            UUID id,
            UUID paymentMethodId,
            UUID cashSessionId,
            BigDecimal amount,
            BigDecimal tipAmount,
            PaymentRecordStatus status,
            String externalReference,
            OffsetDateTime createdAt) {
    }

    public record RefundResponse(
            UUID id,
            UUID paymentId,
            UUID cashSessionId,
            BigDecimal amount,
            PaymentRefundStatus status,
            String reason,
            String externalReference,
            OffsetDateTime createdAt) {
    }
}
