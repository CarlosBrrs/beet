package com.beet.backend.modules.supplier.application.handler;

import com.beet.backend.modules.supplier.application.dto.SupplierActivationRequest;
import com.beet.backend.modules.supplier.application.dto.SupplierRequest;
import com.beet.backend.modules.supplier.application.dto.SupplierResponse;
import com.beet.backend.modules.supplier.application.port.out.SupplierQueryPort;
import com.beet.backend.modules.supplier.domain.api.SupplierServicePort;
import com.beet.backend.modules.supplier.domain.exception.SupplierNotFoundException;
import com.beet.backend.modules.supplier.domain.model.SupplierDomain;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierHandlerImpl implements SupplierHandler {

    private final SupplierServicePort servicePort;
    private final SupplierQueryPort queryPort;

    @Override
    public ApiGenericResponse<List<SupplierResponse>> findAllActive(UUID ownerId) {
        return ApiGenericResponse.success(queryPort.findAllActiveByOwnerId(ownerId));
    }

    @Override
    public ApiGenericResponse<PageResponse<SupplierResponse>> list(
            UUID ownerId, int page, int size, String search, Boolean active, String sortBy, boolean sortDesc) {
        return ApiGenericResponse.success(
                queryPort.findAllByOwnerId(ownerId, page, size, search, active, sortBy, sortDesc));
    }

    @Override
    public ApiGenericResponse<SupplierResponse> findById(UUID supplierId, UUID ownerId) {
        SupplierResponse response = queryPort.findResponseById(supplierId, ownerId)
                .orElseThrow(() -> SupplierNotFoundException.forId(supplierId));
        return ApiGenericResponse.success(response);
    }

    @Override
    public ApiGenericResponse<SupplierResponse> create(SupplierRequest request, UUID ownerId) {
        SupplierDomain created = servicePort.create(toDomain(request), ownerId);
        return findById(created.getId(), ownerId);
    }

    @Override
    public ApiGenericResponse<SupplierResponse> update(UUID supplierId, SupplierRequest request, UUID ownerId) {
        SupplierDomain updated = servicePort.update(supplierId, toDomain(request), ownerId);
        return findById(updated.getId(), ownerId);
    }

    @Override
    public ApiGenericResponse<SupplierResponse> setActive(UUID supplierId, SupplierActivationRequest request, UUID ownerId) {
        SupplierDomain updated = servicePort.setActive(supplierId, request.isActive(), ownerId);
        return findById(updated.getId(), ownerId);
    }

    @Override
    public ApiGenericResponse<Void> delete(UUID supplierId, UUID ownerId, UUID actorId) {
        servicePort.delete(supplierId, ownerId, actorId);
        return ApiGenericResponse.success(null);
    }

    private SupplierDomain toDomain(SupplierRequest request) {
        return SupplierDomain.builder()
                .name(request.name())
                .documentTypeId(request.documentTypeId())
                .documentNumber(request.documentNumber())
                .contactName(request.contactName())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .build();
    }
}
