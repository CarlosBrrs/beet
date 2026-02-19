package com.beet.backend.modules.restaurant.domain.model;

import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class UserRestaurantPermissions {
    private final UUID restaurantId;
    private final UUID userId;
    private final String roleName;
    private final UUID roleId;
    private final Map<PermissionModule, List<PermissionAction>> permissions;
}
