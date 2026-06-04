package com.beet.backend.modules.order.infrastructure.output.adapter;

import com.beet.backend.modules.order.domain.spi.OrderTableGateway;
import com.beet.backend.modules.table.domain.api.RestaurantTableServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderTableGatewayAdapter implements OrderTableGateway {
    private final RestaurantTableServicePort tableService;

    @Override
    public void validateAvailableForOrder(UUID restaurantId, UUID tableId) {
        tableService.validateAvailableForOrder(restaurantId, tableId);
    }
}
