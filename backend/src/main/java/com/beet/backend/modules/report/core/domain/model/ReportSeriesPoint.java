package com.beet.backend.modules.report.core.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportSeriesPoint(
        LocalDate period,
        BigDecimal grossSales,
        BigDecimal completedSales,
        BigDecimal collected,
        BigDecimal refunds,
        long orderCount) {
}
