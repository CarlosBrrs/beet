package com.beet.backend.modules.restaurant.infrastructure.output.adapter;

import com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.aggregate.Permissions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.RowMapper;
import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class PermissionRowMapper implements RowMapper<PermissionProjection> {

    private final ObjectMapper objectMapper;

    public PermissionRowMapper() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public PermissionProjection mapRow(ResultSet rs, int rowNum) throws SQLException {
        PermissionProjection projection = new PermissionProjection();
        projection.setId(rs.getString("id"));
        projection.setRoleName(rs.getString("role_name"));
        projection.setRoleId(rs.getObject("role_id", UUID.class));
        projection.setUserId(rs.getObject("user_id", UUID.class));
        projection.setRestaurantId(rs.getObject("restaurant_id", UUID.class));

        // Manual JSON conversion
        String json = rs.getString("permission");
        if (json != null) {
            try {
                Map<PermissionModule, List<PermissionAction>> map = objectMapper.readValue(
                        json,
                        new TypeReference<Map<PermissionModule, List<PermissionAction>>>() {
                        });
                projection.setPermission(new Permissions(map));
            } catch (JsonProcessingException e) {
                log.error("Error parsing permissions JSON", e);
                projection.setPermission(null);
            }
        }

        return projection;
    }
}
