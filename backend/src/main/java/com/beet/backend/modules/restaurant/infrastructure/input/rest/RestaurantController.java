package com.beet.backend.modules.restaurant.infrastructure.input.rest;

import com.beet.backend.modules.restaurant.application.dto.RestaurantRequest;
import com.beet.backend.modules.restaurant.application.dto.RestaurantResponse;
import com.beet.backend.modules.restaurant.application.dto.RestaurantUpdateRequest;
import com.beet.backend.modules.restaurant.application.dto.UserRestaurantPermissionsResponse;
import com.beet.backend.modules.restaurant.application.handler.RestaurantHandler;
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
@RequestMapping("/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantHandler handler;

    @PostMapping
    public ResponseEntity<ApiGenericResponse<RestaurantResponse>> create(
            @Valid @RequestBody RestaurantRequest request) {
        UUID ownerId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(handler.create(request, ownerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiGenericResponse<RestaurantResponse>> getById(@PathVariable UUID id) {
        UUID ownerId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(handler.getById(id, ownerId));
    }

    @GetMapping("/{id}/my-permissions")
    public ResponseEntity<ApiGenericResponse<UserRestaurantPermissionsResponse>> getPermissions(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(handler.getPermissions(id, userId));
    }

    @GetMapping("/my-restaurants")
    public ResponseEntity<ApiGenericResponse<List<RestaurantResponse>>> getMyRestaurants() {
        UUID ownerId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(handler.getRestaurantsByOwner(ownerId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiGenericResponse<RestaurantResponse>> update(
            @PathVariable UUID id,
            @RequestBody RestaurantUpdateRequest request) {
        UUID ownerId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(handler.update(id, request, ownerId));
    }

}
