package com.beet.backend.modules.inventory.infrastructure.output.persistence.jdbc.repository;

import com.beet.backend.modules.inventory.infrastructure.output.persistence.jdbc.aggregate.IngredientStockAggregate;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IngredientStockJdbcRepository extends ListCrudRepository<IngredientStockAggregate, UUID> {

    boolean existsByMasterIngredientIdAndRestaurantId(UUID masterIngredientId, UUID restaurantId);
}
