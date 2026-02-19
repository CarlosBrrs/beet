package com.beet.backend.modules.restaurant.infrastructure.output.adapter;

import java.util.UUID;
import com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.aggregate.Permissions;
import lombok.Data;

@Data
public class PermissionProjection {

    private String id;
    private String roleName;
    private UUID roleId;
    private UUID userId;
    private UUID restaurantId;
    private Permissions permission;
}
