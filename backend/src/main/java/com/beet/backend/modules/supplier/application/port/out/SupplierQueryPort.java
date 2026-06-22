package com.beet.backend.modules.supplier.application.port.out;

import com.beet.backend.modules.supplier.application.dto.SupplierResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application-level Out Port for CQRS read operations.
 * Bypasses the Domain layer to directly fetch view models (DTOs).
 */
public interface SupplierQueryPort {
    List<SupplierResponse> findAllActiveByOwnerId(UUID ownerId);

    PageResponse<SupplierResponse> findAllByOwnerId(
            UUID ownerId, int page, int size, String search, Boolean active, String sortBy, boolean sortDesc);

    Optional<SupplierResponse> findResponseById(UUID supplierId, UUID ownerId);
}
