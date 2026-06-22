package com.beet.backend.modules.report.catalog.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogReportRow(
        UUID restaurantId,
        String restaurantName,
        UUID referenceId,
        String name,
        String lineType,
        BigDecimal soldQuantity,
        BigDecimal canceledQuantity,
        BigDecimal grossSales,
        BigDecimal theoreticalCost,
        BigDecimal theoreticalMargin,
        boolean costComplete,
        String optionBreakdown) {
}
