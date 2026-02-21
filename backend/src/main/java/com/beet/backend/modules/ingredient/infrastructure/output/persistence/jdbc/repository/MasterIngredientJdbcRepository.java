package com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.repository;

import com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.aggregate.MasterIngredientAggregate;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MasterIngredientJdbcRepository extends ListCrudRepository<MasterIngredientAggregate, UUID> {

    boolean existsByNameAndOwnerId(String name, UUID ownerId);
}
