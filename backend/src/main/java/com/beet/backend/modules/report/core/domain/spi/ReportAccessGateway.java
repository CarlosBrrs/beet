package com.beet.backend.modules.report.core.domain.spi;

import com.beet.backend.modules.role.domain.model.PermissionModule;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ReportAccessGateway {
    List<UUID> accessibleRestaurants(
            UUID userId,
            List<UUID> requestedRestaurantIds,
            Set<PermissionModule> permissions);
}
