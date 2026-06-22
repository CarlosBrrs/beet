package com.beet.backend.modules.report.inventory.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record LowStockReportRow(
        UUID restaurantId,
        String restaurantName,
        UUID ingredientId,
        String ingredientName,
        BigDecimal currentStock,
        BigDecimal minStock,
        BigDecimal shortage) {
}
