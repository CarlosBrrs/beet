package com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.role.domain.model.RoleDomain;
import com.beet.backend.modules.role.domain.model.UserRoleDTO;
import com.beet.backend.modules.role.domain.spi.RolePersistencePort;
import com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.mapper.RoleAggregateMapper;
import com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.repository.RoleJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
}
