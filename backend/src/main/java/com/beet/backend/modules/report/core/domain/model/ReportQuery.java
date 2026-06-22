package com.beet.backend.modules.report.core.domain.model;

import java.time.LocalDate;
import java.util.UUID;

public record ReportQuery(
        ReportScope scope,
        LocalDate dateFrom,
        LocalDate dateTo,
        ReportGrouping grouping,
        int page,
        int size,
        String sort,
        String search,
        UUID resourceId) {
}
