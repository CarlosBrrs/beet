package com.beet.backend.modules.report.core.domain.usecase;

import com.beet.backend.modules.report.core.domain.api.ReportProvider;
import com.beet.backend.modules.report.core.domain.api.ReportServicePort;
import com.beet.backend.modules.report.core.domain.model.*;
import com.beet.backend.modules.report.core.domain.spi.ReportAccessGateway;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportUseCase implements ReportServicePort {
    private static final int MAX_RANGE_DAYS = 366;
    private static final int MAX_PAGE_SIZE = 100;

    private final ReportRegistry registry;
    private final ReportAccessGateway accessGateway;

    @Override
    public <R extends ReportResult> R execute(
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
            Class<R> resultType) {
        ReportProvider<? extends ReportResult> provider = registry.provider(key);
        LocalDate end = dateTo == null ? LocalDate.now() : dateTo;
        LocalDate start = dateFrom == null ? end.minusDays(29) : dateFrom;
        validateRange(start, end);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        validateSort(provider, sort);

        List<UUID> restaurantIds;
        if (context == ReportContext.RESTAURANT) {
            if (requestedRestaurantIds == null || requestedRestaurantIds.size() != 1) {
                throw new IllegalArgumentException("Restaurant reports require exactly one restaurant.");
            }
            restaurantIds = List.copyOf(requestedRestaurantIds);
        } else {
            Set<PermissionModule> permissions = new HashSet<>(provider.additionalPermissions());
            permissions.add(provider.requiredPermission());
            restaurantIds = accessGateway.accessibleRestaurants(userId, requestedRestaurantIds, permissions);
        }

        ReportQuery query = new ReportQuery(
                new ReportScope(context, restaurantIds), start, end,
                grouping == null ? ReportGrouping.DAY : grouping,
                safePage, safeSize, sort, normalize(search), resourceId);
        return registry.execute(key, query, resultType);
    }

    private void validateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("dateFrom cannot be after dateTo.");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("Report date range cannot exceed 366 days.");
        }
    }

    private void validateSort(ReportProvider<? extends ReportResult> provider, String sort) {
        if (sort == null || sort.isBlank()) {
            return;
        }
        String field = sort.split(",", 2)[0];
        if (!provider.sortableFields().contains(field)) {
            throw new IllegalArgumentException("Unsupported report sort field: " + field);
        }
    }

    private String normalize(String search) {
        return search == null || search.isBlank() ? null : search.trim();
    }
}
