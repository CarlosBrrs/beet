package com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.repository;

import com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.aggregate.Permissions;

import java.util.UUID;

public record UserRolePermissionProjection(
        UUID urrRestaurantId,
        String roleName,
        String rolePresetKey,
        UUID roleTemplateRestaurantId,
        Permissions permissions) {
}
