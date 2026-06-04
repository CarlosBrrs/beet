package com.beet.backend.modules.item.infrastructure.input.rest;

import com.beet.backend.modules.item.application.dto.*;
import com.beet.backend.modules.item.application.handler.ItemHandler;
import com.beet.backend.modules.role.domain.model.*;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.security.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/restaurants/{restaurantId}/products")
@RequiredArgsConstructor
public class ProductController {
    private final ItemHandler handler;
    @GetMapping @RequiresPermission(module=PermissionModule.PRODUCTS, action=PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<PageResponse<ItemResponse>>> list(
            @PathVariable UUID restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search){
        return ResponseEntity.ok(handler.getAllProducts(restaurantId, page, size, search));
    }
    @PostMapping @RequiresPermission(module=PermissionModule.PRODUCTS, action=PermissionAction.CREATE)
    public ResponseEntity<ApiGenericResponse<ItemResponse>> create(@PathVariable UUID restaurantId,@Valid @RequestBody CreateProductRequest request){ return ResponseEntity.status(HttpStatus.CREATED).body(handler.createProduct(restaurantId,request)); }
    @GetMapping("/template-options") @RequiresPermission(module=PermissionModule.TEMPLATES, action=PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<List<ItemResponse>>> options(@PathVariable UUID restaurantId){ return ResponseEntity.ok(handler.getTemplateOptions(restaurantId)); }
    @GetMapping("/{productId}") @RequiresPermission(module=PermissionModule.PRODUCTS, action=PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<ItemResponse>> get(@PathVariable UUID restaurantId,@PathVariable UUID productId){ return ResponseEntity.ok(handler.getById(restaurantId,productId)); }
    @GetMapping("/{productId}/dependencies") @RequiresPermission(module=PermissionModule.PRODUCTS, action=PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<ProductDependenciesResponse>> dependencies(@PathVariable UUID restaurantId,@PathVariable UUID productId){ return ResponseEntity.ok(handler.getProductDependencies(restaurantId,productId)); }
    @PutMapping("/{productId}") @RequiresPermission(module=PermissionModule.PRODUCTS, action=PermissionAction.EDIT)
    public ResponseEntity<ApiGenericResponse<ItemResponse>> update(@PathVariable UUID restaurantId,@PathVariable UUID productId,@Valid @RequestBody UpdateItemRequest request){ return ResponseEntity.ok(handler.updateItem(restaurantId,productId,request)); }
    @PatchMapping("/{productId}/activation") @RequiresPermission(module=PermissionModule.PRODUCTS, action=PermissionAction.EDIT)
    public ResponseEntity<ApiGenericResponse<ItemResponse>> activate(@PathVariable UUID restaurantId,@PathVariable UUID productId,@Valid @RequestBody ActivationRequest request){ return ResponseEntity.ok(handler.setProductActive(restaurantId,productId,request.isActive())); }
}
