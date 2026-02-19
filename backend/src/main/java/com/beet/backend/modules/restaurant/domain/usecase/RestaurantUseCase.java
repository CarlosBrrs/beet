package com.beet.backend.modules.restaurant.domain.usecase;

import com.beet.backend.modules.restaurant.domain.api.RestaurantServicePort;
import com.beet.backend.modules.restaurant.domain.exception.RestaurantAlreadyExistsException;
import com.beet.backend.modules.restaurant.domain.exception.RestaurantNotFoundException;
import com.beet.backend.modules.restaurant.domain.exception.RestaurantLimitExceededException;
import com.beet.backend.modules.restaurant.domain.exception.RoleAssignmentException;
import com.beet.backend.modules.restaurant.domain.model.RestaurantDomain;
import com.beet.backend.modules.restaurant.domain.spi.RestaurantPersistencePort;
import com.beet.backend.modules.restaurant.domain.spi.RestaurantSubscriptionGateway;
import com.beet.backend.modules.restaurant.domain.spi.RestaurantIdentityGateway;
import com.beet.backend.modules.restaurant.domain.model.RestaurantWithRole;
import com.beet.backend.modules.role.domain.model.UserRoleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantUseCase implements RestaurantServicePort {

    private final RestaurantPersistencePort persistencePort;
    private final RestaurantSubscriptionGateway subscriptionGateway;
    private final RestaurantIdentityGateway identityGateway;

    @Override
    @Transactional
    public RestaurantWithRole create(RestaurantDomain domain) {
        // 1. Check Max Restaurants Limit
        int maxAllowed = subscriptionGateway.getMaxRestaurantsAllowed(domain.getOwnerId());
        int currentCount = persistencePort.countByOwnerId(domain.getOwnerId());

        if (currentCount >= maxAllowed) {
            throw RestaurantLimitExceededException.forPlan(maxAllowed);
        }

        // 2. Check Unique Constraints per Owner
        if (persistencePort.existsByNameAndOwnerId(domain.getName(), domain.getOwnerId())) {
            throw RestaurantAlreadyExistsException.forField("name", domain.getName());
        }
        if (persistencePort.existsByAddressAndOwnerId(domain.getAddress(), domain.getOwnerId())) {
            throw RestaurantAlreadyExistsException.forField("address", domain.getAddress());
        }
        if (persistencePort.existsByPhoneNumberAndOwnerId(domain.getPhoneNumber(), domain.getOwnerId())) {
            throw RestaurantAlreadyExistsException.forField("phone number", domain.getPhoneNumber());
        }

        RestaurantDomain savedRestaurant = persistencePort.save(domain);

        // 3. Assign 'Owner' Role
        try {
            identityGateway.assignRole(savedRestaurant.getOwnerId(), savedRestaurant.getId(), "Owner");
        } catch (Exception e) {
            throw RoleAssignmentException.forRole("Owner", e);
        }
return new RestaurantWithRole(
                savedRestaurant.getId(),
                savedRestaurant.getName(),
                savedRestaurant.getOperationMode(),
                savedRestaurant.getIsActive(),
                savedRestaurant.getOwnerId(),
                savedRestaurant.getSettings(),
                "Owner");
    }

    @Override
    public RestaurantDomain getById(UUID id, UUID ownerId) {
        return persistencePort.findById(id)
                .filter(r -> r.getOwnerId().equals(ownerId))
                .orElseThrow(() -> RestaurantNotFoundException.forId(id));
    }

    @Override
    public RestaurantWithRole getByIdWithRole(UUID id, UUID userId) {
        // 1. Fetch Restaurant
        RestaurantDomain restaurant = persistencePort.findById(id)
                .orElseThrow(() -> RestaurantNotFoundException.forId(id));

        // 2. Fetch User Roles
        List<UserRoleDTO> userRoles = identityGateway.getUserRoles(userId);

        // 3. Find Role for this Restaurant
        String roleName = userRoles.stream()
                .filter(ur -> ur.restaurantId().equals(id))
                .findFirst()
                .map(UserRoleDTO::roleName)
                .orElse(null);

        // 4. Access Control: If role is null (no association) and not owner
        // Note: The previous logic strictly checked ownership.
        // If we want to allow employees to view it, we should check if roleName is
        // present.
        // If roleName is null and user is not owner, access denied?
        // For now, if role is null, we return it with null role, but maybe we should
        // validation access.
        // The previous getById filtered by ownerId.
        if (roleName == null && !restaurant.getOwnerId().equals(userId)) {
            // Treat as not found or access denied
            throw RestaurantNotFoundException.forId(id);
        }

        // If owner, role might be "Owner" if implicitly assigned, or we default it.
        if (roleName == null && restaurant.getOwnerId().equals(userId)) {
            roleName = "Owner";
        }

        return new RestaurantWithRole(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getOperationMode(),
                restaurant.getIsActive(),
                restaurant.getOwnerId(),
                restaurant.getSettings(),
                roleName);
    }

    @Override
    public List<RestaurantDomain> getRestaurantsByOwner(UUID ownerId) {
        return persistencePort.findAllByOwnerId(ownerId);
    }

    @Override
    public List<RestaurantWithRole> getRestaurantsWithRole(UUID userId) {
        // 1. Fetch User Roles from Role Module via Gateway
        List<UserRoleDTO> userRoles = identityGateway.getUserRoles(userId);

        if (userRoles.isEmpty()) {
            return List.of();
        }

        // 2. Extract Restaurant IDs
        List<UUID> restaurantIds = userRoles.stream()
                .map(UserRoleDTO::restaurantId)
                .toList();

        // 3. Fetch Restaurants by IDs
        List<RestaurantDomain> restaurants = persistencePort.findAllById(restaurantIds);

        // 4. Combine in Memory
        return restaurants.stream()
                .map(restaurant -> {
                    String roleName = userRoles.stream()
                            .filter(ur -> ur.restaurantId().equals(restaurant.getId()))
                            .findFirst()
                            .map(UserRoleDTO::roleName)
                            .orElse("Unknown");

                    return new RestaurantWithRole(
                            restaurant.getId(),
                            restaurant.getName(),
                            restaurant.getOperationMode(),
                            restaurant.getIsActive(),
                            restaurant.getOwnerId(),
                            restaurant.getSettings(),
                            roleName);
                })
                .toList();
    }

    @Override
    @Transactional
    public RestaurantDomain update(RestaurantDomain domain) {
        // 1. Fetch existing and check ownership
        RestaurantDomain existing = getById(domain.getId(), domain.getOwnerId());

        // 2. Merge changes
        RestaurantDomain merged = existing.toBuilder()
                .name(domain.getName() != null ? domain.getName() : existing.getName())
                .address(domain.getAddress() != null ? domain.getAddress() : existing.getAddress())
                .email(domain.getEmail() != null ? domain.getEmail() : existing.getEmail())
                .phoneNumber(domain.getPhoneNumber() != null ? domain.getPhoneNumber() : existing.getPhoneNumber())
                .operationMode(
                        domain.getOperationMode() != null ? domain.getOperationMode() : existing.getOperationMode())
                .isActive(domain.getIsActive() != null ? domain.getIsActive() : existing.getIsActive())
                .settings(domain.getSettings() != null ? domain.getSettings() : existing.getSettings())
                .build();

        // 3. Validate Unique Constraints (if changed)
        if (!merged.getName().equals(existing.getName()) &&
                persistencePort.existsByNameAndOwnerId(merged.getName(), merged.getOwnerId())) {
            throw RestaurantAlreadyExistsException.forField("name", merged.getName());
        }
        if (!merged.getAddress().equals(existing.getAddress()) &&
                persistencePort.existsByAddressAndOwnerId(merged.getAddress(), merged.getOwnerId())) {
            throw RestaurantAlreadyExistsException.forField("address", merged.getAddress());
        }
        if (!merged.getPhoneNumber().equals(existing.getPhoneNumber()) &&
                persistencePort.existsByPhoneNumberAndOwnerId(merged.getPhoneNumber(), merged.getOwnerId())) {
            throw RestaurantAlreadyExistsException.forField("phone number", merged.getPhoneNumber());
        }

        // 4. Save
        return persistencePort.save(merged);
    }

    @Override
    @Transactional
    public RestaurantWithRole updateWithRole(RestaurantDomain domain, UUID userId) {
        RestaurantDomain updated = update(domain);

        // Compose with role
        // For update, we can assume we want the role of the user performing the update
        List<UserRoleDTO> userRoles = identityGateway.getUserRoles(userId);

        String roleName = userRoles.stream()
                .filter(ur -> ur.restaurantId().equals(updated.getId()))
                .findFirst()
                .map(UserRoleDTO::roleName)
                .orElse(null);

        // Fallback for owner if something desynced or race condition
        if (roleName == null && updated.getOwnerId().equals(userId)) {
            roleName = "Owner";
        }

        return new RestaurantWithRole(
                updated.getId(),
                updated.getName(),
                updated.getOperationMode(),
                updated.getIsActive(),
                updated.getOwnerId(),
                updated.getSettings(),
                roleName);
    }

    @Override
    public boolean existsById(UUID id) {
        return persistencePort.existsById(id);
    }

}
