package com.beet.backend.modules.order.application.dto;

import com.beet.backend.modules.order.domain.model.CatalogReferenceType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PosCatalogResponse(
        UUID nodeId,
        UUID menuId,
        String menuName,
        UUID submenuId,
        String submenuName,
        CatalogReferenceType referenceType,
        UUID referenceId,
        String name,
        String description,
        BigDecimal price,
        boolean available,
        boolean lowStock,
        String unavailableReason,
        int sortOrder,
        List<String> insufficientIngredients,
        List<PosTemplateSlotResponse> slots) {

    public record PosTemplateSlotResponse(
            UUID slotId,
            String name,
            int minSelection,
            int maxSelection,
            int sortOrder,
            List<PosTemplateOptionResponse> options) {
    }

    public record PosTemplateOptionResponse(
            UUID slotOptionId,
            UUID itemId,
            String itemName,
            BigDecimal surcharge,
            int maxQuantity,
            boolean isDefault,
            boolean available,
            boolean lowStock,
            String unavailableReason,
            int sortOrder,
            List<String> insufficientIngredients) {
    }
}
