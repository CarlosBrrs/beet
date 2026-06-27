package com.beet.backend.modules.report.core.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReportOverview(
        LocalDate dateFrom,
        LocalDate dateTo,
        boolean provisional,
        BigDecimal grossSales,
        BigDecimal completedSales,
        BigDecimal openOrderValue,
        long completedOrders,
        BigDecimal averageTicket,
        BigDecimal collected,
        BigDecimal tips,
        BigDecimal refunds,
        BigDecimal netCollected,
        List<ServiceTypeTotal> serviceTypes,
        List<RestaurantTotal> restaurants) implements ReportResult {

    public record ServiceTypeTotal(String serviceType, long orderCount, BigDecimal grossSales) {
    }

    public record RestaurantTotal(
            java.util.UUID restaurantId,
            String restaurantName,
            BigDecimal grossSales,
            BigDecimal collected,
            long completedOrders) {
    }
}
