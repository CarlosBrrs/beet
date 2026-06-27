package com.beet.backend.modules.inventory.infrastructure.input.rest;

import com.beet.backend.modules.inventory.application.dto.*;
import com.beet.backend.modules.inventory.application.handler.InventoryHandler;
import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.security.RequiresPermission;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Inventory endpoints are scoped to a specific restaurant.
 * Authorization is enforced via @RequiresPermission,
 * which reads restaurantId from the path and checks the user's role
 * permissions.
 */
@RestController
@RequestMapping("/restaurants/{restaurantId}/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryHandler handler;

    @PostMapping("/activate")
    @RequiresPermission(module = PermissionModule.INVENTORY, action = PermissionAction.ACTIVATE)
    public ResponseEntity<ApiGenericResponse<InventoryStockResponse>> activate(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody ActivateIngredientRequest request) {

        UUID userId = SecurityUtils.getAuthenticatedUserId();
        UUID ownerId = SecurityUtils.getEffectiveOwnerId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(handler.activate(request, restaurantId, ownerId, userId));
    }

    @GetMapping
    @RequiresPermission(module = PermissionModule.INVENTORY, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<PageResponse<InventoryStockResponse>>> listStocks(
            @PathVariable UUID restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ingredientName") String sortBy,
            @RequestParam(defaultValue = "false") boolean sortDesc) {

        return ResponseEntity.ok(handler.listStocks(restaurantId, page, size, search, sortBy, sortDesc));
    }

    @GetMapping("/available")
    @RequiresPermission(module = PermissionModule.INVENTORY, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<List<InventoryStockResponse>>> listAvailable(
            @PathVariable UUID restaurantId) {

        UUID ownerId = SecurityUtils.getEffectiveOwnerId();
        return ResponseEntity.ok(handler.listAvailable(restaurantId, ownerId));
    }

    @PutMapping("/{stockId}/adjust")
    @RequiresPermission(module = PermissionModule.INVENTORY, action = PermissionAction.EDIT)
    public ResponseEntity<ApiGenericResponse<InventoryTransactionResponse>> adjustStock(
            @PathVariable UUID restaurantId,
            @PathVariable UUID stockId,
            @Valid @RequestBody AdjustStockRequest request) {

        UUID userId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(handler.adjustStock(restaurantId, stockId, request, userId));
    }

    @GetMapping("/{stockId}/transactions")
    @RequiresPermission(module = PermissionModule.INVENTORY, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<PageResponse<InventoryTransactionResponse>>> getTransactions(
            @PathVariable UUID restaurantId,
            @PathVariable UUID stockId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(handler.getTransactions(stockId, page, size));
    }
}
