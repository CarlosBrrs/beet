package com.beet.backend.modules.order.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record IngredientStockAvailabilityDomain(
        UUID masterIngredientId,
        BigDecimal currentStock,
        BigDecimal minStock,
        BigDecimal activeReserved) {

    public BigDecimal availableStock() {
        return currentStock.subtract(activeReserved);
    }
}
