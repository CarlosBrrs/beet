package com.beet.backend.modules.ingredient.application.port.out;

import com.beet.backend.modules.ingredient.application.dto.IngredientDetailResponse;
import com.beet.backend.modules.ingredient.application.dto.IngredientListResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application-level Out Port for CQRS read operations.
 * Bypasses the Domain layer to directly fetch view models (DTOs) for
 * ingredients.
 */
public interface IngredientQueryPort {

    PageResponse<IngredientListResponse> findAllByOwnerId(
            UUID ownerId, int page, int size,
            String search, String sortBy, boolean sortDesc, List<String> units);

    Optional<IngredientDetailResponse> findDetailById(UUID id, UUID ownerId);
}
