package com.beet.backend.modules.table.application.handler;

import com.beet.backend.modules.table.application.dto.CreateRestaurantTableRequest;
import com.beet.backend.modules.table.application.dto.RestaurantTableResponse;
import com.beet.backend.modules.table.application.dto.UpdateRestaurantTableRequest;
import com.beet.backend.modules.table.domain.api.RestaurantTableServicePort;
import com.beet.backend.modules.table.domain.model.RestaurantTableDomain;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantTableHandlerImpl implements RestaurantTableHandler {
    private final RestaurantTableServicePort service;

    @Override
    public ApiGenericResponse<RestaurantTableResponse> create(
            UUID restaurantId, CreateRestaurantTableRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        RestaurantTableDomain created = service.create(RestaurantTableDomain.builder()
                .restaurantId(restaurantId)
                .name(request.name())
                .capacity(request.capacity())
                .area(request.area())
                .sortOrder(request.sortOrder())
                .notes(request.notes())
                .createdBy(userId)
                .updatedBy(userId)
                .build());
        return ApiGenericResponse.success(toResponse(created));
    }

    @Override
    public ApiGenericResponse<List<RestaurantTableResponse>> list(UUID restaurantId) {
        return ApiGenericResponse.success(service.listByRestaurant(restaurantId).stream()
                .map(this::toResponse)
                .toList());
    }

    @Override
    public ApiGenericResponse<RestaurantTableResponse> update(
            UUID restaurantId, UUID tableId, UpdateRestaurantTableRequest request) {
        RestaurantTableDomain updated = service.update(RestaurantTableDomain.builder()
                .id(tableId)
                .restaurantId(restaurantId)
                .name(request.name())
                .capacity(request.capacity())
                .area(request.area())
                .sortOrder(request.sortOrder())
                .isActive(request.isActive())
                .notes(request.notes())
                .updatedBy(SecurityUtils.getAuthenticatedUserId())
                .build());
        return ApiGenericResponse.success(toResponse(updated));
    }

    @Override
    public ApiGenericResponse<RestaurantTableResponse> deactivate(UUID restaurantId, UUID tableId) {
        RestaurantTableDomain deactivated = service.deactivate(
                restaurantId, tableId, SecurityUtils.getAuthenticatedUserId());
        return ApiGenericResponse.success(toResponse(deactivated));
    }

    private RestaurantTableResponse toResponse(RestaurantTableDomain table) {
        return new RestaurantTableResponse(
                table.getId(),
                table.getRestaurantId(),
                table.getName(),
                table.getCapacity(),
                table.getArea(),
                table.getSortOrder(),
                Boolean.TRUE.equals(table.getIsActive()),
                table.getNotes(),
                table.getAvailabilityStatus(),
                table.getOpenOrderId(),
                table.getCreatedAt(),
                table.getUpdatedAt());
    }
}
