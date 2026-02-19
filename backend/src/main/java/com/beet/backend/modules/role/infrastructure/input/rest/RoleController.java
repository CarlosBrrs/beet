package com.beet.backend.modules.role.infrastructure.input.rest;

import com.beet.backend.modules.role.application.handler.RoleHandler;
import com.beet.backend.modules.role.application.dto.RoleContextResponse;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleHandler handler;

    @GetMapping("/permissions")
    public ResponseEntity<ApiGenericResponse<RoleContextResponse>> getPermissions(
            @RequestParam UUID restaurantId) {
        return ResponseEntity.ok(handler.getPermissions(restaurantId));
    }
}
