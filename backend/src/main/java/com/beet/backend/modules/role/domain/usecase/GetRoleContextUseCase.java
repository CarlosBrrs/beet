package com.beet.backend.modules.role.domain.usecase;

import com.beet.backend.modules.role.domain.exception.RoleNotFoundException;
import com.beet.backend.modules.role.domain.model.RoleDomain;

import com.beet.backend.modules.role.domain.spi.RolePersistencePort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetRoleContextUseCase {

    private final RolePersistencePort persistencePort;

    public RoleDomain invoke(UUID userId, UUID restaurantId) {
        return persistencePort.findRoleByUserIdAndRestaurantId(userId, restaurantId)
                .orElseThrow(() -> RoleNotFoundException.forUserInRestaurant(userId, restaurantId));
    }
}
