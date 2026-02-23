package com.beet.backend.modules.ingredient.domain.spi;

import com.beet.backend.modules.ingredient.application.dto.IngredientDetailResponse;
import com.beet.backend.modules.ingredient.application.dto.IngredientListResponse;
import com.beet.backend.modules.ingredient.domain.model.MasterIngredientDomain;
import com.beet.backend.modules.ingredient.domain.model.SupplierItemDomain;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngredientPersistencePort {

    MasterIngredientDomain saveMasterIngredient(MasterIngredientDomain ingredient);

    SupplierItemDomain saveSupplierItem(SupplierItemDomain supplierItem);

    void updateActiveSupplierItem(UUID masterIngredientId, UUID supplierItemId);

    boolean existsByNameAndOwnerId(String name, UUID ownerId);

    PageResponse<IngredientListResponse> findAllByOwnerId(
            UUID ownerId, int page, int size,
            String search, String sortBy, boolean sortDesc, List<String> units);

    Optional<IngredientDetailResponse> findDetailById(UUID id, UUID ownerId);
}
