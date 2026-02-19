package com.beet.backend.modules.role.domain.api;

import java.util.UUID;

public interface AssignRoleServicePort {
    void assignRole(UUID userId, UUID restaurantId, String roleName, UUID senderId);
}
