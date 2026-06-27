package com.beet.backend.modules.order.application.dto;

import com.beet.backend.modules.order.domain.model.KitchenTicketStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record KitchenTicketEventResponse(
        String eventType,
        UUID restaurantId,
        UUID ticketId,
        UUID orderId,
        String orderDisplayCode,
        String customerName,
        KitchenTicketStatus status,
        OffsetDateTime occurredAt) {
}