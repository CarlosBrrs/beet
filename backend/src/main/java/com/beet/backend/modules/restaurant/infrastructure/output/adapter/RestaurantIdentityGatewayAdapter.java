package com.beet.backend.modules.restaurant.infrastructure.output.adapter;

import com.beet.backend.modules.restaurant.domain.spi.RestaurantIdentityGateway;
import com.beet.backend.modules.role.domain.api.AssignRoleServicePort;
import com.beet.backend.modules.role.domain.model.UserRoleDTO;
import com.beet.backend.modules.role.domain.spi.RolePersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RestaurantIdentityGatewayAdapter implements RestaurantIdentityGateway {

    private final AssignRoleServicePort assignRoleServicePort;
    private final RolePersistencePort rolePersistencePort;

    @Override
    public void assignRole(UUID userId, UUID restaurantId, String roleName) {
        assignRoleServicePort.assignRole(userId, restaurantId, roleName, userId);
    }

    @Override
    public List<UserRoleDTO> getUserRoles(UUID userId) {
        return rolePersistencePort.findUserRoles(userId);
    }
}
