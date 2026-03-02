package com.beet.backend.modules.menu.infrastructure.input.rest;

import com.beet.backend.modules.menu.application.dto.CreateSubmenuRequest;
import com.beet.backend.modules.menu.application.dto.SubmenuResponse;
import com.beet.backend.modules.menu.application.dto.UpdateSubmenuRequest;
import com.beet.backend.modules.menu.application.handler.MenuHandler;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.security.RequiresPermission;
import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/restaurants/{restaurantId}/menus/{menuId}/submenus")
@RequiredArgsConstructor
public class SubmenuController {

    private final MenuHandler menuHandler;

    @PostMapping
    @RequiresPermission(module = PermissionModule.MENUS, action = PermissionAction.CREATE)
    public ResponseEntity<ApiGenericResponse<SubmenuResponse>> createSubmenu(
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuId,
            @Valid @RequestBody CreateSubmenuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuHandler.createSubmenu(menuId, request));
    }

    @PutMapping("/{submenuId}")
    @RequiresPermission(module = PermissionModule.MENUS, action = PermissionAction.EDIT)
    public ResponseEntity<ApiGenericResponse<SubmenuResponse>> updateSubmenu(
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuId,
            @PathVariable UUID submenuId,
            @Valid @RequestBody UpdateSubmenuRequest request) {
        return ResponseEntity.ok(menuHandler.updateSubmenu(submenuId, request));
    }
}
