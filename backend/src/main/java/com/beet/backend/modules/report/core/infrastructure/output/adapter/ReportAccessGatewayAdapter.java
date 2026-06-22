package com.beet.backend.modules.report.core.infrastructure.output.adapter;

import com.beet.backend.modules.report.core.domain.spi.ReportAccessGateway;
import com.beet.backend.modules.restaurant.domain.api.RestaurantServicePort;
import com.beet.backend.modules.role.application.dto.UserPermissionEntry;
import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import com.beet.backend.modules.role.domain.spi.RolePersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReportAccessGatewayAdapter implements ReportAccessGateway {
    private final RestaurantServicePort restaurantService;
    private final RolePersistencePort rolePersistence;

    @Override
    public List<UUID> accessibleRestaurants(
            UUID userId, List<UUID> requestedRestaurantIds, Set<PermissionModule> requiredPermissions) {
        List<UserPermissionEntry> permissions = rolePersistence.findAllPermissionsForUser(userId);
        boolean owner = permissions.stream().anyMatch(entry ->
                entry.restaurantId() == null
                        && entry.permissions().getOrDefault(PermissionModule.ALL, List.of())
                        .contains(PermissionAction.ALL));

        Set<UUID> accessible = owner
                ? restaurantService.getRestaurantsByOwner(userId).stream()
                    .map(restaurant -> restaurant.getId())
                    .collect(Collectors.toSet())
                : restaurantService.getRestaurantsWithRole(userId).stream()
                    .map(restaurant -> restaurant.id())
                    .collect(Collectors.toSet());
        if (!owner) {
            Set<UUID> permitted = permissions.stream()
                    .filter(entry -> entry.restaurantId() != null)
                    .filter(entry -> requiredPermissions.stream().allMatch(permission ->
                            hasViewPermission(entry, permission)))
                    .map(UserPermissionEntry::restaurantId)
                    .collect(Collectors.toSet());
            accessible.retainAll(permitted);
        }

        if (requestedRestaurantIds != null && !requestedRestaurantIds.isEmpty()) {
            if (!accessible.containsAll(requestedRestaurantIds)) {
                throw new IllegalArgumentException("One or more restaurants are not accessible.");
            }
            return requestedRestaurantIds.stream().distinct().toList();
        }
        if (accessible.isEmpty()) {
            throw new IllegalArgumentException("No accessible restaurants for this report.");
        }
        return accessible.stream().sorted().toList();
    }

    private boolean hasViewPermission(UserPermissionEntry entry, PermissionModule permission) {
        List<PermissionAction> actions = entry.permissions().getOrDefault(permission, List.of());
        return actions.contains(PermissionAction.VIEW)
                || actions.contains(PermissionAction.ALL)
                || actions.contains(PermissionAction.MANAGE);
    }
}
