package com.beet.backend.modules.restaurant.infrastructure.output.adapter;

import com.beet.backend.modules.restaurant.domain.model.UserRestaurantPermissions;
import com.beet.backend.modules.restaurant.domain.spi.RestaurantPermissionsGateway;
import com.beet.backend.modules.restaurant.infrastructure.output.mapper.RestaurantPermissionProjectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RestaurantPermissionsGatewayAdapter implements RestaurantPermissionsGateway {

    private final RestaurantPermissionRepository repository;
    private final RestaurantPermissionProjectionMapper mapper;

    @Override
    public UserRestaurantPermissions getMyPermissionsForRestaurant(UUID restaurantId, UUID userId) {
        var projection = repository.findByRestaurantIdAndUserId(restaurantId, userId);
        if (projection != null) {
            return mapper.toDomain(projection);
        }
        return null;
    }

}
