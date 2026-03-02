package com.beet.backend.modules.restaurant.application.handler;

import com.beet.backend.modules.restaurant.application.dto.RestaurantRequest;
import com.beet.backend.modules.restaurant.application.dto.RestaurantResponse;
import com.beet.backend.modules.restaurant.application.dto.RestaurantUpdateRequest;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;

import java.util.List;
import java.util.UUID;

public interface RestaurantHandler {
    ApiGenericResponse<RestaurantResponse> create(RestaurantRequest request, UUID ownerId);

    ApiGenericResponse<RestaurantResponse> getById(UUID id, UUID ownerId);

    ApiGenericResponse<List<RestaurantResponse>> getRestaurantsByOwner(UUID ownerId);

    ApiGenericResponse<RestaurantResponse> update(UUID id, RestaurantUpdateRequest request, UUID ownerId);
}
