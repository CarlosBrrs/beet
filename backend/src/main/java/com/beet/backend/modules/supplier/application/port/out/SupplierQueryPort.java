package com.beet.backend.modules.supplier.application.port.out;

import com.beet.backend.modules.supplier.application.dto.SupplierResponse;

import java.util.List;
import java.util.UUID;

/**
 * Application-level Out Port for CQRS read operations.
 * Bypasses the Domain layer to directly fetch view models (DTOs).
 */
public interface SupplierQueryPort {
    List<SupplierResponse> findAllActiveByOwnerId(UUID ownerId);
}
