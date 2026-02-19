package com.beet.backend.modules.role.domain.model;

import java.util.UUID;

public record UserRoleDTO(
        UUID restaurantId,
        String roleName) {
}
