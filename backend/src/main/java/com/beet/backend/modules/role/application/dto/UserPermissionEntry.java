package com.beet.backend.modules.role.application.dto;

import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a single permission scope for a user.
 * - restaurantId is null → global role (e.g. OWNER).
 * - restaurantId is set → role scoped to a specific restaurant.
 */
public record UserPermissionEntry(
        UUID restaurantId,
        String role,
        Map<PermissionModule, List<PermissionAction>> permissions) {
}
