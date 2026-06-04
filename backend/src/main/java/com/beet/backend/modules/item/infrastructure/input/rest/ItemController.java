package com.beet.backend.modules.item.infrastructure.input.rest;

import com.beet.backend.modules.item.application.dto.ItemResponse;
import com.beet.backend.modules.item.application.handler.ItemHandler;
import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.security.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for querying Items across a restaurant (read-only hub endpoint).
 * Handles GET /restaurants/{restaurantId}/items
 *
 * Note: Product creation is handled by SubmenuController, not here.
 * This controller exists strictly for global read operations (Information Hub).
 */
@RestController
@RequestMapping("/restaurants/{restaurantId}/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemHandler itemHandler;

    /**
     * Returns all PRODUCT items for a restaurant.
     * Used by the global Products Hub page to display a read-only overview.
     */
    @GetMapping
    @RequiresPermission(module = PermissionModule.PRODUCTS, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<PageResponse<ItemResponse>>> getAllProducts(
            @PathVariable UUID restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(itemHandler.getAllProducts(restaurantId, page, size, search));
    }

    /**
     * Returns a single item by its ID.
     * Used by the ProductHubDetail sheet to display the recipe composition.
     */
    @GetMapping("/{itemId}")
    @RequiresPermission(module = PermissionModule.PRODUCTS, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<ItemResponse>> getById(
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(itemHandler.getById(restaurantId, itemId));
    }
}
