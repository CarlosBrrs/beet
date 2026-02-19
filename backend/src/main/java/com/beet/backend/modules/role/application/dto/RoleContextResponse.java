package com.beet.backend.modules.role.application.dto;

import com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.aggregate.Permissions;
import java.util.UUID;

public record RoleContextResponse(
                UUID restaurantId,
                String role,
                Permissions permissions) {
}
