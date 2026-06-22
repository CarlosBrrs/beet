package com.beet.backend.modules.ingredient.application.handler;

import com.beet.backend.modules.ingredient.application.dto.CreateIngredientRequest;
import com.beet.backend.modules.ingredient.application.dto.IngredientDetailResponse;
import com.beet.backend.modules.ingredient.application.dto.IngredientListResponse;
import com.beet.backend.modules.ingredient.application.dto.IngredientResponse;
import com.beet.backend.modules.ingredient.application.dto.UpdateIngredientRequest;
import com.beet.backend.modules.ingredient.application.mapper.IngredientServiceMapper;
import com.beet.backend.modules.ingredient.application.port.out.IngredientQueryPort;
import com.beet.backend.modules.ingredient.domain.api.IngredientServicePort;
import com.beet.backend.modules.ingredient.domain.exception.IngredientNotFoundException;
import com.beet.backend.modules.ingredient.domain.model.MasterIngredientDomain;
import com.beet.backend.modules.ingredient.domain.model.SupplierItemDomain;
import com.beet.backend.modules.supplier.domain.model.SupplierDomain;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IngredientHandlerImpl implements IngredientHandler {

    private final IngredientServicePort servicePort;
    private final IngredientQueryPort queryPort;
    private final IngredientServiceMapper mapper;

    @Override
    public ApiGenericResponse<IngredientResponse> create(CreateIngredientRequest request, UUID ownerId) {
        MasterIngredientDomain ingredient = mapper.toIngredientDomain(request.masterIngredient());
        SupplierDomain supplier = mapper.toSupplierDomain(request.supplier());
        SupplierItemDomain supplierItem = mapper.toSupplierItemDomain(request.supplierItem());

        MasterIngredientDomain created = servicePort.create(
                ingredient,
                supplier,
                supplierItem,
                request.supplierItem().conversionUnitId(),
                request.supplierItem().conversionFactor(),
                request.supplierItem().totalPrice(),
                ownerId);

        return ApiGenericResponse.success(mapper.toResponse(created, supplierItem));
    }

    @Override
    public ApiGenericResponse<PageResponse<IngredientListResponse>> list(
            UUID ownerId, int page, int size,
            String search, String sortBy, boolean sortDesc, List<String> units) {
        return ApiGenericResponse.success(
                queryPort.findAllByOwnerId(ownerId, page, size, search, sortBy, sortDesc, units));
    }

    @Override
    public ApiGenericResponse<IngredientDetailResponse> findById(UUID id, UUID ownerId) {
        IngredientDetailResponse detail = queryPort.findDetailById(id, ownerId)
                .orElseThrow(() -> IngredientNotFoundException.forId(id));
        return ApiGenericResponse.success(detail);
    }

    @Override
    public ApiGenericResponse<IngredientDetailResponse> update(UUID id, UpdateIngredientRequest request, UUID ownerId) {
        MasterIngredientDomain domain = MasterIngredientDomain.builder()
                .name(request.name())
                .baseUnitId(request.baseUnitId())
                .build();
        servicePort.update(id, domain, ownerId);
        return findById(id, ownerId);
    }

    @Override
    public ApiGenericResponse<Void> delete(UUID id, UUID ownerId, UUID actorId) {
        servicePort.delete(id, ownerId, actorId);
        return ApiGenericResponse.success(null);
    }
}
