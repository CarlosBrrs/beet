package com.beet.backend.modules.ingredient.application.handler;

import com.beet.backend.modules.ingredient.application.dto.CreateIngredientRequest;
import com.beet.backend.modules.ingredient.application.dto.IngredientResponse;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;

import java.util.UUID;

public interface IngredientHandler {
    ApiGenericResponse<IngredientResponse> create(CreateIngredientRequest request, UUID ownerId);
}
