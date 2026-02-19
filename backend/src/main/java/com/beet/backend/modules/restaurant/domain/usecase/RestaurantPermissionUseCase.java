package com.beet.backend.modules.restaurant.domain.usecase;

import com.beet.backend.modules.restaurant.domain.api.RestaurantPermissionsServicePort;
import com.beet.backend.modules.restaurant.domain.exception.RestaurantNotFoundException;
import com.beet.backend.modules.restaurant.domain.model.UserRestaurantPermissions;
import com.beet.backend.modules.restaurant.domain.spi.RestaurantPermissionsGateway;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantPermissionUseCase implements RestaurantPermissionsServicePort {

    private final RestaurantPermissionsGateway restaurantPermissionsGateway;
    private final RestaurantUseCase restaurantUseCase;

    @Override
    public UserRestaurantPermissions getMyPermissionsForRestaurant(UUID restaurantId, UUID userId) {
        if (!restaurantUseCase.existsById(restaurantId)) {
            throw RestaurantNotFoundException.forId(restaurantId);
        }
        return restaurantPermissionsGateway.getMyPermissionsForRestaurant(restaurantId, userId);
    }

}
