package com.beet.backend.modules.menu.infrastructure.input.rest;

import com.beet.backend.modules.item.application.dto.CreateProductRequest;
import com.beet.backend.modules.item.application.dto.ItemResponse;
import com.beet.backend.modules.item.application.dto.UpdateItemRequest;
import com.beet.backend.modules.item.application.handler.ItemHandler;
import com.beet.backend.modules.menu.application.dto.CreateSubmenuRequest;
import com.beet.backend.modules.menu.application.dto.SubmenuNodeResponse;
import com.beet.backend.modules.menu.application.dto.SubmenuResponse;
import com.beet.backend.modules.menu.application.dto.UpdateSubmenuRequest;
import com.beet.backend.modules.menu.application.dto.PublishSubmenuNodeRequest;
import com.beet.backend.modules.menu.application.handler.MenuHandler;
import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import com.beet.backend.modules.template.application.dto.CreateTemplateRequest;
import com.beet.backend.modules.template.application.dto.TemplateResponse;
import com.beet.backend.modules.template.application.handler.TemplateHandler;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.security.RequiresPermission;
import com.beet.backend.shared.infrastructure.security.PermissionRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/restaurants/{restaurantId}/menus/{menuId}/submenus")
@RequiredArgsConstructor
public class SubmenuController {

    private final MenuHandler menuHandler;
    private final ItemHandler itemHandler;
    private final TemplateHandler templateHandler;

    // ---- Submenu CRUD -------------------------------------------------------

    @PostMapping
    @RequiresPermission(module = PermissionModule.MENUS, action = PermissionAction.CREATE)
    public ResponseEntity<ApiGenericResponse<SubmenuResponse>> createSubmenu(
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuId,
            @Valid @RequestBody CreateSubmenuRequest request) {
        menuHandler.assertMenuPath(restaurantId, menuId);
        return ResponseEntity.status(HttpStatus.CREATED).body(menuHandler.createSubmenu(menuId, request));
    }

    @PutMapping("/{submenuId}")
    @RequiresPermission(module = PermissionModule.MENUS, action = PermissionAction.EDIT)
    public ResponseEntity<ApiGenericResponse<SubmenuResponse>> updateSubmenu(
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuId,
            @PathVariable UUID submenuId,
            @Valid @RequestBody UpdateSubmenuRequest request) {
        menuHandler.assertSubmenuPath(restaurantId, menuId, submenuId);
        return ResponseEntity.ok(menuHandler.updateSubmenu(submenuId, request));
    }

    // ---- Products (Capa 2) — always created from their Submenu --------------

    /**
     * Creates a PRODUCT and atomically links it to this Submenu.
     * POST /restaurants/{rId}/menus/{mId}/submenus/{submenuId}/products
     */
    @PostMapping("/{submenuId}/products")
    @RequiresPermission(
            module = PermissionModule.PRODUCTS,
            action = PermissionAction.CREATE,
            additional = @PermissionRequirement(module = PermissionModule.MENUS, action = PermissionAction.EDIT))
    public ResponseEntity<ApiGenericResponse<ItemResponse>> createProduct(
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuId,
            @PathVariable UUID submenuId,
            @Valid @RequestBody CreateProductRequest request) {
        menuHandler.assertSubmenuPath(restaurantId, menuId, submenuId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itemHandler.createProduct(submenuId, restaurantId, request));
    }

    @GetMapping("/{submenuId}/products/{productId}")
    @RequiresPermission(module = PermissionModule.MENUS, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<ItemResponse>> getProduct(
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuId,
            @PathVariable UUID submenuId,
            @PathVariable UUID productId) {
        menuHandler.assertSubmenuPath(restaurantId, menuId, submenuId);
        return ResponseEntity.ok(itemHandler.getById(restaurantId, productId));
    }

    @PutMapping("/{submenuId}/products/{productId}")
    @RequiresPermission(module = PermissionModule.PRODUCTS, action = PermissionAction.EDIT)
    public ResponseEntity<ApiGenericResponse<ItemResponse>> updateProduct(
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuId,
            @PathVariable UUID submenuId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateItemRequest request) {
        menuHandler.assertSubmenuPath(restaurantId, menuId, submenuId);
        return ResponseEntity.ok(itemHandler.updateItem(restaurantId, productId, request));
    }

    /**
     * Returns all sellable nodes (Products + Templates) for a Submenu.
     * GET /restaurants/{rId}/menus/{mId}/submenus/{submenuId}/nodes
     */
    @GetMapping("/{submenuId}/nodes")
    @RequiresPermission(module = PermissionModule.MENUS, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<List<SubmenuNodeResponse>>> getNodes(
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuId,
            @PathVariable UUID submenuId) {
        return ResponseEntity.ok(menuHandler.getSubmenuNodes(restaurantId, menuId, submenuId));
    }

    @PostMapping("/{submenuId}/nodes")
    @RequiresPermission(module = PermissionModule.MENUS, action = PermissionAction.EDIT)
    public ResponseEntity<ApiGenericResponse<SubmenuNodeResponse>> publishNode(
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuId,
            @PathVariable UUID submenuId,
            @Valid @RequestBody PublishSubmenuNodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(menuHandler.publishSubmenuNode(restaurantId, menuId, submenuId, request));
    }

    @DeleteMapping("/{submenuId}/nodes/{nodeId}")
    @RequiresPermission(module = PermissionModule.MENUS, action = PermissionAction.EDIT)
    public ResponseEntity<ApiGenericResponse<Void>> deleteNode(
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuId,
            @PathVariable UUID submenuId,
            @PathVariable UUID nodeId) {
        return ResponseEntity.ok(menuHandler.deleteSubmenuNode(restaurantId, menuId, submenuId, nodeId));
    }

    /**
     * Creates a TEMPLATE (combo) and atomically links it to this Submenu.
     * POST /restaurants/{rId}/menus/{mId}/submenus/{submenuId}/templates
     */
    @PostMapping("/{submenuId}/templates")
    @RequiresPermission(
            module = PermissionModule.TEMPLATES,
            action = PermissionAction.CREATE,
            additional = @PermissionRequirement(module = PermissionModule.MENUS, action = PermissionAction.EDIT))
    public ResponseEntity<ApiGenericResponse<TemplateResponse>> createTemplate(
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuId,
            @PathVariable UUID submenuId,
            @Valid @RequestBody CreateTemplateRequest request) {
        menuHandler.assertSubmenuPath(restaurantId, menuId, submenuId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(templateHandler.createTemplate(submenuId, restaurantId, request));
    }
}
