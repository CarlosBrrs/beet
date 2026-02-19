package com.beet.backend.modules.restaurant.application.dto;

import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record UserRestaurantPermissionsResponse(
        UUID restaurantId,
        UUID userId,
        String roleName,
        UUID roleId,
        Map<PermissionModule, List<PermissionAction>> permissions) {
}
