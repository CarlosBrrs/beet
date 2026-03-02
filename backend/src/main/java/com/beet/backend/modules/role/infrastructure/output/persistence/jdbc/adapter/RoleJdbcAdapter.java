package com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.role.application.dto.UserPermissionEntry;
import com.beet.backend.modules.role.domain.exception.RoleConflictException;
import com.beet.backend.modules.role.domain.model.RoleDomain;
import com.beet.backend.modules.role.domain.model.UserRoleDTO;
import com.beet.backend.modules.role.domain.spi.RolePersistencePort;
import com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.mapper.RoleAggregateMapper;
import com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.repository.RoleJdbcRepository;
import com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.repository.UserRolePermissionProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RoleJdbcAdapter implements RolePersistencePort {

    private final RoleAggregateMapper mapper;
    private final RoleJdbcRepository repository;

    @Override
    public Optional<UUID> findRoleIdByName(String name) {
        return repository.findIdByName(name);
    }

    @Override
    public boolean existsByUserIdAndRestaurantIdAndRoleId(UUID userId, UUID restaurantId, UUID roleId) {
        return repository.existsByUserIdAndRestaurantIdAndRoleId(userId, restaurantId, roleId);
    }

    @Override
    public void assignRoleToUser(UUID userId, UUID restaurantId, UUID roleId, UUID senderId) {
        repository.assignRoleToUser(userId, restaurantId, roleId, senderId);
    }

    @Override
    public List<UserRoleDTO> findUserRoles(UUID userId) {
        return repository.findUserRoles(userId);
    }

    @Override
    public Optional<RoleDomain> findRoleByUserIdAndRestaurantId(UUID userId,
            UUID restaurantId) {
        return repository.findRoleByUserIdAndRestaurantId(userId, restaurantId)
                .map(mapper::toDomain);
    }

    @Override
    public List<UserPermissionEntry> findAllPermissionsForUser(UUID userId) {
        List<UserRolePermissionProjection> rows = repository.findAllRoleAssignmentsForUser(userId);
        List<UserPermissionEntry> result = new ArrayList<>();
        boolean ownerAdded = false;

        // Integrity check: a user cannot have both OWNER and operative roles
        boolean hasOwnerRole = rows.stream().anyMatch(r -> "OWNER".equals(r.roleName()));
        boolean hasOtherRole = rows.stream().anyMatch(r -> !"OWNER".equals(r.roleName()));
        if (hasOwnerRole && hasOtherRole) {
            throw RoleConflictException.ownerWithOperativeRoles();
        }

        for (UserRolePermissionProjection row : rows) {
            // Only the OWNER role is global — it gets collapsed into a single entry with
            // restaurantId = null.
            // All other roles (MANAGER, CASHIER, etc.) are per-restaurant even if their
            // role template
            // also has restaurant_id = NULL in the roles table (because they're
            // system-defined roles).
            boolean isOwner = "OWNER".equals(row.roleName());

            if (isOwner) {
                if (!ownerAdded) {
                    result.add(new UserPermissionEntry(
                            null,
                            row.roleName(),
                            row.permissions().getModulePermissions()));
                    ownerAdded = true;
                }
            } else {
                // Per-restaurant role: one entry per restaurant assignment
                result.add(new UserPermissionEntry(
                        row.urrRestaurantId(),
                        row.roleName(),
                        row.permissions().getModulePermissions()));
            }
        }
        return result;
    }
}
