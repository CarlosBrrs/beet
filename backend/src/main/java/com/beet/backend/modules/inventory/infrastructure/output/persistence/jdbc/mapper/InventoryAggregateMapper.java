package com.beet.backend.modules.inventory.infrastructure.output.persistence.jdbc.mapper;

import com.beet.backend.modules.inventory.domain.model.InventoryStockDomain;
import com.beet.backend.modules.inventory.infrastructure.output.persistence.jdbc.aggregate.IngredientStockAggregate;
import org.springframework.stereotype.Component;

@Component
public class InventoryAggregateMapper {

    public IngredientStockAggregate toAggregate(InventoryStockDomain domain) {
        return IngredientStockAggregate.builder()
                .id(domain.getId()) // null on create → DB generates
                .masterIngredientId(domain.getMasterIngredientId())
                .restaurantId(domain.getRestaurantId())
                .currentStock(domain.getCurrentStock())
                .minStock(domain.getMinStock())
                .build();
    }

    public InventoryStockDomain toDomain(IngredientStockAggregate aggregate) {
        return InventoryStockDomain.builder()
                .id(aggregate.getId())
                .masterIngredientId(aggregate.getMasterIngredientId())
                .restaurantId(aggregate.getRestaurantId())
                .currentStock(aggregate.getCurrentStock())
                .minStock(aggregate.getMinStock())
                .build();
    }
}
