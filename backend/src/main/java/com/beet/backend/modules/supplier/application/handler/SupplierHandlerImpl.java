package com.beet.backend.modules.supplier.application.handler;

import com.beet.backend.modules.supplier.application.dto.SupplierResponse;
import com.beet.backend.modules.supplier.application.port.out.SupplierQueryPort;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierHandlerImpl implements SupplierHandler {

    private final SupplierQueryPort queryPort;

    @Override
    public ApiGenericResponse<List<SupplierResponse>> findAllActive(UUID ownerId) {
        List<SupplierResponse> response = queryPort.findAllActiveByOwnerId(ownerId);
        return ApiGenericResponse.success(response);
    }
}
