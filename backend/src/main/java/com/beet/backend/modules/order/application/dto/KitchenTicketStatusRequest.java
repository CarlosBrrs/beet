package com.beet.backend.modules.order.application.dto;

import com.beet.backend.modules.order.domain.model.InventoryReversalDecision;
import com.beet.backend.modules.order.domain.model.KitchenTicketStatus;

public record KitchenTicketStatusRequest(
        KitchenTicketStatus status,
        InventoryReversalDecision inventoryDecision,
        String notes) {
}
