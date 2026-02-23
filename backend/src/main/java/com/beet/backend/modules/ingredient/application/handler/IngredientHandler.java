package com.beet.backend.modules.ingredient.application.handler;

import com.beet.backend.modules.ingredient.application.dto.CreateIngredientRequest;
import com.beet.backend.modules.ingredient.application.dto.IngredientDetailResponse;
import com.beet.backend.modules.ingredient.application.dto.IngredientListResponse;
import com.beet.backend.modules.ingredient.application.dto.IngredientResponse;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.util.List;
import java.util.UUID;

public interface IngredientHandler {
    ApiGenericResponse<IngredientResponse> create(CreateIngredientRequest request, UUID ownerId);

    ApiGenericResponse<PageResponse<IngredientListResponse>> list(
            UUID ownerId, int page, int size,
            String search, String sortBy, boolean sortDesc, List<String> units);

    ApiGenericResponse<IngredientDetailResponse> findById(UUID id, UUID ownerId);
}
