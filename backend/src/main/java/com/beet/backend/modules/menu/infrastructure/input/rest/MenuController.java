package com.beet.backend.modules.menu.infrastructure.input.rest;

import com.beet.backend.modules.menu.application.dto.CreateMenuRequest;
import com.beet.backend.modules.menu.application.dto.MenuResponse;
import com.beet.backend.modules.menu.application.dto.UpdateMenuRequest;
import com.beet.backend.modules.menu.application.handler.MenuHandler;
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

@RestController
@RequestMapping("/restaurants/{restaurantId}/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuHandler menuHandler;

    @PostMapping
    @RequiresPermission(module = PermissionModule.MENUS, action = PermissionAction.CREATE)
    public ResponseEntity<ApiGenericResponse<MenuResponse>> createMenu(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody CreateMenuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuHandler.createMenu(restaurantId, request));
    }

    @PutMapping("/{menuId}")
    @RequiresPermission(module = PermissionModule.MENUS, action = PermissionAction.EDIT)
    public ResponseEntity<ApiGenericResponse<MenuResponse>> updateMenu(
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuId,
            @Valid @RequestBody UpdateMenuRequest request) {
        return ResponseEntity.ok(menuHandler.updateMenu(menuId, request));
    }

    @GetMapping
    @RequiresPermission(module = PermissionModule.MENUS, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<List<MenuResponse>>> findAllMenus(@PathVariable UUID restaurantId) {
        return ResponseEntity.ok(menuHandler.findAllMenus(restaurantId));
    }
}
