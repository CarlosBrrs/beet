package com.beet.backend.modules.restaurant.application.handler;

import com.beet.backend.modules.restaurant.application.dto.RestaurantRequest;
import com.beet.backend.modules.restaurant.application.dto.RestaurantResponse;
import com.beet.backend.modules.restaurant.application.dto.RestaurantUpdateRequest;
import com.beet.backend.modules.restaurant.application.dto.UserRestaurantPermissionsResponse;
import com.beet.backend.modules.restaurant.application.mapper.RestaurantServiceMapper;
import com.beet.backend.modules.restaurant.application.mapper.UserRestaurantPermissionsMapper;
import com.beet.backend.modules.restaurant.domain.api.RestaurantPermissionsServicePort;
import com.beet.backend.modules.restaurant.domain.api.RestaurantServicePort;
import com.beet.backend.modules.restaurant.domain.model.RestaurantDomain;
import com.beet.backend.modules.restaurant.domain.model.RestaurantWithRole;
import com.beet.backend.modules.restaurant.domain.model.UserRestaurantPermissions;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RestaurantHandlerImpl implements RestaurantHandler {

    private final RestaurantServicePort servicePort;
    private final RestaurantPermissionsServicePort permissionsServicePort;
    private final RestaurantServiceMapper mapper;
    private final UserRestaurantPermissionsMapper permissionsMapper;

    @Override
    public ApiGenericResponse<RestaurantResponse> create(RestaurantRequest request, UUID ownerId) {
        RestaurantDomain domain = mapper.toDomain(request, ownerId);
        RestaurantWithRole created = servicePort.create(domain);
        return ApiGenericResponse.success(mapper.toResponse(created));
    }

    @Override
    public ApiGenericResponse<RestaurantResponse> getById(UUID id, UUID ownerId) {
        RestaurantWithRole found = servicePort.getByIdWithRole(id, ownerId);
        return ApiGenericResponse.success(mapper.toResponse(found));
    }

    @Override
    public ApiGenericResponse<List<RestaurantResponse>> getRestaurantsByOwner(UUID ownerId) {
        // Now using the "WithRole" method which covers both Owners and Employees
        // ownerId here is actually the userId from the token
        var restaurants = servicePort.getRestaurantsWithRole(ownerId);
        List<RestaurantResponse> response = restaurants.stream()
                .map(mapper::toResponse)
                .toList();
        return ApiGenericResponse.success(response);
    }

    @Override
    public ApiGenericResponse<RestaurantResponse> update(UUID id, RestaurantUpdateRequest request, UUID ownerId) {
        RestaurantDomain partialUpdate = mapper.toDomain(request, id, ownerId);
        RestaurantWithRole updated = servicePort.updateWithRole(partialUpdate, ownerId);
        return ApiGenericResponse.success(mapper.toResponse(updated));
    }

    @Override
    public ApiGenericResponse<UserRestaurantPermissionsResponse> getPermissions(UUID restaurantId, UUID userId) {
        UserRestaurantPermissions permissions = permissionsServicePort.getMyPermissionsForRestaurant(restaurantId,
                userId);
        return ApiGenericResponse.success(permissionsMapper.toResponse(permissions));
    }
}
