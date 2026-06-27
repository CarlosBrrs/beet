package com.beet.backend.modules.report.core.domain.api;

import com.beet.backend.modules.report.core.domain.model.ReportContext;
import com.beet.backend.modules.report.core.domain.model.ReportGrouping;
import com.beet.backend.modules.report.core.domain.model.ReportResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReportServicePort {
    <R extends ReportResult> R execute(
            String key,
            ReportContext context,
            UUID userId,
            List<UUID> requestedRestaurantIds,
            LocalDate dateFrom,
            LocalDate dateTo,
            ReportGrouping grouping,
            int page,
            int size,
            String sort,
            String search,
            UUID resourceId,
            Class<R> resultType);
}
