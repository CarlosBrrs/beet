package com.beet.backend.modules.table.infrastructure.output.adapter;

import com.beet.backend.modules.restaurant.domain.exception.RestaurantNotFoundException;
import com.beet.backend.modules.restaurant.domain.model.RestaurantDomain;
import com.beet.backend.modules.restaurant.domain.spi.RestaurantPersistencePort;
import com.beet.backend.modules.table.domain.spi.RestaurantTableRestaurantGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RestaurantTableRestaurantGatewayAdapter implements RestaurantTableRestaurantGateway {
    private final RestaurantPersistencePort restaurantPersistence;

    @Override
    public Integer getMaxTableCapacity(UUID restaurantId) {
        RestaurantDomain restaurant = restaurantPersistence.findById(restaurantId)
                .orElseThrow(() -> RestaurantNotFoundException.forId(restaurantId));
        return restaurant.getSettings() != null ? restaurant.getSettings().maxTableCapacity() : null;
    }
}
