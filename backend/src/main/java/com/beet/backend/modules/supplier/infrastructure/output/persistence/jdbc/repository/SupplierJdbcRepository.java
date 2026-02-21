package com.beet.backend.modules.supplier.infrastructure.output.persistence.jdbc.repository;

import com.beet.backend.modules.supplier.infrastructure.output.persistence.jdbc.aggregate.SupplierAggregate;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SupplierJdbcRepository extends ListCrudRepository<SupplierAggregate, UUID> {

    boolean existsByOwnerIdAndDocumentTypeIdAndDocumentNumber(UUID ownerId, UUID documentTypeId, String documentNumber);
}
