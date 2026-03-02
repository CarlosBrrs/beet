package com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.repository;

import com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.aggregate.Permissions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Maps a SQL row from the full role assignment query into a
 * UserRolePermissionProjection.
 * Uses a static ObjectMapper since Spring Data JDBC requires a no-arg
 * constructor for rowMapperClass.
 */
public class UserRolePermissionRowMapper implements RowMapper<UserRolePermissionProjection> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public UserRolePermissionProjection mapRow(ResultSet rs, int rowNum) throws SQLException {
        String urrRestaurantIdStr = rs.getString("urr_restaurant_id");
        UUID urrRestaurantId = urrRestaurantIdStr != null ? UUID.fromString(urrRestaurantIdStr) : null;

        String roleRestaurantIdStr = rs.getString("role_template_restaurant_id");
        UUID roleTemplateRestaurantId = roleRestaurantIdStr != null ? UUID.fromString(roleRestaurantIdStr) : null;

        String roleName = rs.getString("role_name");

        Permissions permissions;
        try {
            String permissionsJson = rs.getString("permissions");
            permissions = OBJECT_MAPPER.readValue(permissionsJson, Permissions.class);
        } catch (Exception e) {
            throw new SQLException("Failed to parse permissions JSON for role: " + roleName, e);
        }

        return new UserRolePermissionProjection(urrRestaurantId, roleName, roleTemplateRestaurantId, permissions);
    }
}
