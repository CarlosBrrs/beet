package com.beet.backend.modules.order.application.dto;

import com.beet.backend.modules.order.domain.model.ServiceType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreateRequest(
        ServiceType serviceType,
        UUID tableId,
        String customerName,
        String customerPhone,
        String deliveryContactName,
        String deliveryPhone,
        String deliveryAddress,
        String deliveryNotes,
        BigDecimal deliveryFee,
        String notes,
        List<OrderItemRequest> items) {

    public record OrderItemRequest(
            String lineType,
            UUID itemId,
            UUID templateId,
            UUID submenuNodeId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            String notes,
            List<TemplateSlotSelectionRequest> slots) {
    }

    public record TemplateSlotSelectionRequest(
            UUID slotId,
            List<TemplateOptionSelectionRequest> options) {
    }

    public record TemplateOptionSelectionRequest(
            UUID slotOptionId,
            UUID itemId,
            BigDecimal quantity) {
    }
}
