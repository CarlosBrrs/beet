package com.beet.backend.modules.table.application.handler;

import com.beet.backend.modules.table.application.dto.CreateRestaurantTableRequest;
import com.beet.backend.modules.table.application.dto.RestaurantTableResponse;
import com.beet.backend.modules.table.application.dto.UpdateRestaurantTableRequest;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;

import java.util.List;
import java.util.UUID;

public interface RestaurantTableHandler {
    ApiGenericResponse<RestaurantTableResponse> create(UUID restaurantId, CreateRestaurantTableRequest request);

    ApiGenericResponse<List<RestaurantTableResponse>> list(UUID restaurantId);

    ApiGenericResponse<RestaurantTableResponse> update(
            UUID restaurantId, UUID tableId, UpdateRestaurantTableRequest request);

    ApiGenericResponse<RestaurantTableResponse> deactivate(UUID restaurantId, UUID tableId);
}
