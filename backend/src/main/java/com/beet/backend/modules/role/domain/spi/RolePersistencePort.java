package com.beet.backend.modules.role.domain.spi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.beet.backend.modules.role.domain.model.RoleDomain;
import com.beet.backend.modules.role.domain.model.UserRoleDTO;
import com.beet.backend.modules.role.application.dto.UserPermissionEntry;

public interface RolePersistencePort {
    Optional<UUID> findRoleIdByName(String name);

    boolean existsByUserIdAndRestaurantIdAndRoleId(UUID userId, UUID restaurantId, UUID roleId);

    void assignRoleToUser(UUID userId, UUID restaurantId, UUID roleId, UUID senderId);

    List<UserRoleDTO> findUserRoles(UUID userId);

    Optional<RoleDomain> findRoleByUserIdAndRestaurantId(UUID userId, UUID restaurantId);

    /**
     * Fetches all permission scopes for a user.
     * Global roles (e.g. OWNER) are collapsed into a single entry with restaurantId
     * = null.
     */
    List<UserPermissionEntry> findAllPermissionsForUser(UUID userId);
}
