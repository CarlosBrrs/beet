package com.beet.backend.modules.ingredient.infrastructure.input.rest;

import com.beet.backend.modules.ingredient.application.dto.CreateIngredientRequest;
import com.beet.backend.modules.ingredient.application.dto.IngredientDetailResponse;
import com.beet.backend.modules.ingredient.application.dto.IngredientListResponse;
import com.beet.backend.modules.ingredient.application.dto.IngredientResponse;
import com.beet.backend.modules.ingredient.application.handler.IngredientHandler;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Ingredients are scoped to the authenticated owner (global catalog).
 * The restaurantId is NOT part of this path — all data is filtered by ownerId
 * from the JWT.
 */
@RestController
@RequestMapping("/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientHandler handler;

    @PostMapping
    public ResponseEntity<ApiGenericResponse<IngredientResponse>> create(
            @Valid @RequestBody CreateIngredientRequest request) {
        UUID ownerId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(handler.create(request, ownerId));
    }

    @GetMapping
    public ResponseEntity<ApiGenericResponse<PageResponse<IngredientListResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "false") boolean sortDesc,
            @RequestParam(required = false) List<String> unit) {
        UUID ownerId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(handler.list(ownerId, page, size, search, sortBy, sortDesc, unit));
    }

    @GetMapping("/{ingredientId}")
    public ResponseEntity<ApiGenericResponse<IngredientDetailResponse>> getById(
            @PathVariable UUID ingredientId) {
        UUID ownerId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(handler.findById(ingredientId, ownerId));
    }
}
