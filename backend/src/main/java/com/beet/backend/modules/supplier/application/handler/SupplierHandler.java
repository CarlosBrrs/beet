package com.beet.backend.modules.supplier.application.handler;

import com.beet.backend.modules.supplier.application.dto.SupplierResponse;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;

import java.util.List;
import java.util.UUID;

public interface SupplierHandler {
    ApiGenericResponse<List<SupplierResponse>> findAllActive(UUID ownerId);
}
