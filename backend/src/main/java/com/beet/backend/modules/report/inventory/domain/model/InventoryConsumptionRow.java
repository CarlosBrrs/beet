package com.beet.backend.modules.report.inventory.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryConsumptionRow(
        UUID restaurantId,
        String restaurantName,
        UUID ingredientId,
        String ingredientName,
        BigDecimal quantityBase,
        BigDecimal historicalCost,
        boolean costComplete) {
}
