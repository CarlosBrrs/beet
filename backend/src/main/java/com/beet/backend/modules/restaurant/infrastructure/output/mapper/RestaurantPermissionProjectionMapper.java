package com.beet.backend.modules.restaurant.infrastructure.output.mapper;

import com.beet.backend.modules.restaurant.domain.model.UserRestaurantPermissions;
import com.beet.backend.modules.restaurant.infrastructure.output.adapter.PermissionProjection;
import org.springframework.stereotype.Component;

@Component
public class RestaurantPermissionProjectionMapper {

    public UserRestaurantPermissions toDomain(PermissionProjection projection) {
        if (projection == null) {
            return null;
        }
        return UserRestaurantPermissions.builder()
                .restaurantId(projection.getRestaurantId())
                .userId(projection.getUserId())
                .roleId(projection.getRoleId())
                .roleName(projection.getRoleName())
                .permissions(
                        projection.getPermission() != null ? projection.getPermission().getModulePermissions() : null)
                .build();
    }
}
