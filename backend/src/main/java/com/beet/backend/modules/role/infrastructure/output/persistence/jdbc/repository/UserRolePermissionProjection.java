package com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.repository;

import com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.aggregate.Permissions;

import java.util.UUID;

/**
 * Internal projection holding raw SQL row data for the full permission
 * assignment query.
 * Used by UserRolePermissionRowMapper and consumed in RoleJdbcAdapter to build
 * UserPermissionEntry.
 */
public record UserRolePermissionProjection(
        UUID urrRestaurantId,
        String roleName,
        UUID roleTemplateRestaurantId, // null = global role (e.g. OWNER), non-null = custom restaurant role
        Permissions permissions) {
}
