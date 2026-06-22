package com.beet.backend.modules.supplier.domain.spi;

import com.beet.backend.modules.supplier.domain.model.SupplierDomain;

import java.util.Optional;
import java.util.UUID;

public interface SupplierPersistencePort {

    SupplierDomain save(SupplierDomain supplier);

    Optional<SupplierDomain> findById(UUID id);

    Optional<SupplierDomain> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByOwnerAndDocument(UUID ownerId, UUID documentTypeId, String documentNumber);

    boolean existsByOwnerAndDocumentExcludingId(UUID ownerId, UUID documentTypeId, String documentNumber, UUID excludedId);

    boolean hasBlockingReferences(UUID supplierId);

    void updateActive(UUID supplierId, UUID ownerId, boolean isActive);

    void softDelete(UUID supplierId, UUID ownerId, UUID actorId);
}
