package com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.repository;

import com.beet.backend.modules.role.domain.model.UserRoleDTO;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class UserRoleRowMapper implements RowMapper<UserRoleDTO> {
    @Override
    public UserRoleDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new UserRoleDTO(
                UUID.fromString(rs.getString("restaurant_id")),
                rs.getString("role_name"));
    }
}
