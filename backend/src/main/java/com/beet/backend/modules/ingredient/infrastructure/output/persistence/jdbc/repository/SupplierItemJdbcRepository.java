package com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.repository;

import com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.aggregate.SupplierItemAggregate;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SupplierItemJdbcRepository extends ListCrudRepository<SupplierItemAggregate, UUID> {
}
