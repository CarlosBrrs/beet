package com.beet.backend.modules.restaurant.domain.spi;

import java.util.List;
import java.util.UUID;

import com.beet.backend.modules.role.domain.model.UserRoleDTO;

public interface RestaurantIdentityGateway {
    void assignRole(UUID userId, UUID restaurantId, String roleName);

    List<UserRoleDTO> getUserRoles(UUID userId);
}
