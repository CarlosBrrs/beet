package com.beet.backend.modules.role.domain.usecase;

import com.beet.backend.modules.role.domain.api.AssignRoleServicePort;
import com.beet.backend.modules.role.domain.exception.RoleNotFoundException;
import com.beet.backend.modules.role.domain.spi.RolePersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignRoleUseCase implements AssignRoleServicePort {

    private final RolePersistencePort persistencePort;

    @Override
    public void assignRole(UUID userId, UUID restaurantId, String roleName, UUID senderId) {
        UUID roleId = persistencePort.findRoleIdByName(roleName)
                .orElseThrow(() -> RoleNotFoundException.forName(roleName));

        if (persistencePort.existsByUserIdAndRestaurantIdAndRoleId(userId, restaurantId, roleId)) {
            return;
        }

        persistencePort.assignRoleToUser(userId, restaurantId, roleId, senderId);
    }
}
