package com.beet.backend.modules.restaurant.application.mapper;

import com.beet.backend.modules.restaurant.application.dto.UserRestaurantPermissionsResponse;
import com.beet.backend.modules.restaurant.domain.model.UserRestaurantPermissions;
import org.springframework.stereotype.Component;

@Component
public class UserRestaurantPermissionsMapper {

    public UserRestaurantPermissionsResponse toResponse(UserRestaurantPermissions domain) {
        if (domain == null) {
            return null;
        }
        return new UserRestaurantPermissionsResponse(
                domain.getRestaurantId(),
                domain.getUserId(),
                domain.getRoleName(),
                domain.getRoleId(),
                domain.getPermissions());
    }
}
