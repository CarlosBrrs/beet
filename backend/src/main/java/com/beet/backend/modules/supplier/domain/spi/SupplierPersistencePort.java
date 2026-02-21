package com.beet.backend.modules.supplier.domain.spi;

import com.beet.backend.modules.supplier.domain.model.SupplierDomain;

import java.util.Optional;
import java.util.UUID;

public interface SupplierPersistencePort {

    SupplierDomain save(SupplierDomain supplier);

    Optional<SupplierDomain> findById(UUID id);

    boolean existsByOwnerAndDocument(UUID ownerId, UUID documentTypeId, String documentNumber);
}
