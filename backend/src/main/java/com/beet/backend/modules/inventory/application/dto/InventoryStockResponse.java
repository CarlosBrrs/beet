package com.beet.backend.modules.inventory.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryStockResponse(
        UUID id,
        UUID masterIngredientId,
        String ingredientName,
        String unitAbbreviation,
        BigDecimal currentStock,
        BigDecimal minStock,
        boolean lowStock) {
}
