package com.beet.backend.modules.supplier.domain.api;

import com.beet.backend.modules.supplier.domain.model.SupplierDomain;

import java.util.UUID;

/**
 * Used by the ingredient module to resolve or create suppliers during
 * ingredient quick creation, and by the supplier CRUD flow.
 */
public interface SupplierServicePort {
    SupplierDomain findOrCreate(SupplierDomain candidate, UUID ownerId);

    SupplierDomain create(SupplierDomain supplier, UUID ownerId);

    SupplierDomain update(UUID supplierId, SupplierDomain supplier, UUID ownerId);

    SupplierDomain setActive(UUID supplierId, boolean isActive, UUID ownerId);

    void delete(UUID supplierId, UUID ownerId, UUID actorId);
}
