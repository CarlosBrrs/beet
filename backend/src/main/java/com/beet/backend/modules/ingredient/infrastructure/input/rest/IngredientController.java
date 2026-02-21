package com.beet.backend.modules.ingredient.infrastructure.input.rest;

import com.beet.backend.modules.ingredient.application.dto.CreateIngredientRequest;
import com.beet.backend.modules.ingredient.application.dto.IngredientResponse;
import com.beet.backend.modules.ingredient.application.handler.IngredientHandler;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/restaurants/{restaurantId}/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientHandler handler;

    @PostMapping
    public ResponseEntity<ApiGenericResponse<IngredientResponse>> create(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody CreateIngredientRequest request) {
        UUID ownerId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(handler.create(request, ownerId));
    }
}
