package com.beet.backend.modules.item.infrastructure.input.rest;

import com.beet.backend.modules.item.application.dto.CreatePreparationRequest;
import com.beet.backend.modules.item.application.dto.ItemResponse;
import com.beet.backend.modules.item.application.dto.UpdateItemRequest;
import com.beet.backend.modules.item.application.handler.ItemHandler;
import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.security.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Preparations (Capa 1 — internal sub-recipes).
 * Preparations are standalone: they do NOT belong to a Submenu.
 * Base path: /restaurants/{restaurantId}/preparations
 */
@RestController
@RequestMapping("/restaurants/{restaurantId}/preparations")
@RequiredArgsConstructor
public class PreparationController {

    private final ItemHandler itemHandler;

    @PostMapping
    @RequiresPermission(module = PermissionModule.RECIPES, action = PermissionAction.CREATE)
    public ResponseEntity<ApiGenericResponse<ItemResponse>> createPreparation(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody CreatePreparationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itemHandler.createPreparation(restaurantId, request));
    }

    @GetMapping
    @RequiresPermission(module = PermissionModule.RECIPES, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<List<ItemResponse>>> getAllPreparations(
            @PathVariable UUID restaurantId) {
        return ResponseEntity.ok(itemHandler.getAllPreparations(restaurantId));
    }

    @GetMapping("/{itemId}")
    @RequiresPermission(module = PermissionModule.RECIPES, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<ItemResponse>> getById(
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(itemHandler.getById(itemId));
    }

    @PutMapping("/{itemId}")
    @RequiresPermission(module = PermissionModule.RECIPES, action = PermissionAction.EDIT)
    public ResponseEntity<ApiGenericResponse<ItemResponse>> updatePreparation(
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateItemRequest request) {
        return ResponseEntity.ok(itemHandler.updateItem(itemId, request));
    }

    @DeleteMapping("/{itemId}")
    @RequiresPermission(module = PermissionModule.RECIPES, action = PermissionAction.DELETE)
    public ResponseEntity<ApiGenericResponse<Void>> deletePreparation(
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(itemHandler.deleteItem(itemId));
    }
}
