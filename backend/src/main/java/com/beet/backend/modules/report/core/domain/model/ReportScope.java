package com.beet.backend.modules.report.core.domain.model;

import java.util.List;
import java.util.UUID;

public record ReportScope(ReportContext context, List<UUID> restaurantIds) {
    public ReportScope {
        restaurantIds = List.copyOf(restaurantIds);
        if (restaurantIds.isEmpty()) {
            throw new IllegalArgumentException("At least one restaurant is required.");
        }
    }
}
