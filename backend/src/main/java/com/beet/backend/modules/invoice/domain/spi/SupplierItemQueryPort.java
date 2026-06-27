package com.beet.backend.modules.invoice.domain.spi;

import com.beet.backend.modules.ingredient.domain.model.SupplierItemDomain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-module query port for supplier items.
 * Used by the invoice module to read and update supplier item cost data.
 */
public interface SupplierItemQueryPort {

    Optional<SupplierItemDomain> findById(UUID supplierItemId);

    void updateLastCostBase(UUID supplierItemId, BigDecimal newCostBase);

    List<SupplierItemDomain> findBySupplierId(UUID supplierId);
}
