package com.beet.backend.modules.supplier.domain.api;

import com.beet.backend.modules.supplier.domain.model.SupplierDomain;

import java.util.UUID;

/**
 * Service port for supplier operations.
 * Used by the ingredient module to resolve or create suppliers during
 * ingredient creation.
 */
public interface SupplierServicePort {

    /**
     * If candidate.id is not null → looks up existing supplier.
     * If candidate.id is null → creates a new supplier (quick-add).
     */
    SupplierDomain findOrCreate(SupplierDomain candidate, UUID ownerId);
}
