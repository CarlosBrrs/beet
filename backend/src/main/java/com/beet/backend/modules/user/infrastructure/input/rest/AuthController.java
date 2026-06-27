package com.beet.backend.modules.user.infrastructure.input.rest;

import com.beet.backend.modules.role.application.dto.UserPermissionEntry;
import com.beet.backend.modules.user.application.dto.LoginRequest;
import com.beet.backend.modules.user.application.dto.LoginResponse;
import com.beet.backend.modules.user.application.dto.RegisterUserRequest;
import com.beet.backend.modules.user.application.dto.UserResponse;
import com.beet.backend.modules.user.application.handler.AuthHandler;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthHandler authHandler;

    @PostMapping("/register")
    public ResponseEntity<ApiGenericResponse<UserResponse>> register(@Valid @RequestBody RegisterUserRequest request) {
        ApiGenericResponse<UserResponse> response = authHandler.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiGenericResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authHandler.login(request));
    }

    @GetMapping("/my-permissions")
    public ResponseEntity<ApiGenericResponse<List<UserPermissionEntry>>> getMyPermissions() {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(authHandler.getMyPermissions(userId));
    }
}
