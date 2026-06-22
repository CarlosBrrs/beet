package com.beet.backend.modules.supplier.application.handler;

import com.beet.backend.modules.supplier.application.dto.SupplierActivationRequest;
import com.beet.backend.modules.supplier.application.dto.SupplierRequest;
import com.beet.backend.modules.supplier.application.dto.SupplierResponse;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.util.List;
import java.util.UUID;

public interface SupplierHandler {
    ApiGenericResponse<List<SupplierResponse>> findAllActive(UUID ownerId);

    ApiGenericResponse<PageResponse<SupplierResponse>> list(
            UUID ownerId, int page, int size, String search, Boolean active, String sortBy, boolean sortDesc);

    ApiGenericResponse<SupplierResponse> findById(UUID supplierId, UUID ownerId);

    ApiGenericResponse<SupplierResponse> create(SupplierRequest request, UUID ownerId);

    ApiGenericResponse<SupplierResponse> update(UUID supplierId, SupplierRequest request, UUID ownerId);

    ApiGenericResponse<SupplierResponse> setActive(UUID supplierId, SupplierActivationRequest request, UUID ownerId);

    ApiGenericResponse<Void> delete(UUID supplierId, UUID ownerId, UUID actorId);
}
