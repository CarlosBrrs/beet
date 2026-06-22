package com.beet.backend.modules.restaurant.application.mapper;

import com.beet.backend.modules.restaurant.application.dto.RestaurantRequest;
import com.beet.backend.modules.restaurant.application.dto.RestaurantResponse;
import com.beet.backend.modules.restaurant.application.dto.RestaurantUpdateRequest;
import com.beet.backend.modules.restaurant.domain.model.RestaurantDomain;
import com.beet.backend.modules.restaurant.domain.model.RestaurantWithRole;
import com.beet.backend.shared.domain.model.OperationMode;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RestaurantServiceMapper {

    public RestaurantDomain toDomain(RestaurantRequest request, UUID ownerId) {
        return RestaurantDomain.builder()
                .name(request.name())
                .address(request.address())
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .operationMode(OperationMode.valueOf(request.operationMode()))
                .isActive(request.isActive() != null ? request.isActive() : true)
                .ownerId(ownerId)
                .settings(request.settings())
                .build();
    }

    public RestaurantResponse toResponse(RestaurantDomain domain) {
        return RestaurantResponse.builder()
                .id(domain.getId())
                .name(domain.getName())
                .address(domain.getAddress())
                .email(domain.getEmail())
                .phoneNumber(domain.getPhoneNumber())
                .operationMode(domain.getOperationMode().name())
                .isActive(domain.getIsActive())
                .ownerId(domain.getOwnerId())
                .settings(domain.getSettings())
                .build();
    }

    public RestaurantResponse toResponse(RestaurantWithRole dto) {
        return RestaurantResponse.builder()
                .id(dto.id())
                .name(dto.name())
                .address(dto.address())
                .email(dto.email())
                .phoneNumber(dto.phoneNumber())
                .operationMode(dto.operationMode().name())
                .isActive(dto.isActive())
                .ownerId(dto.ownerId())
                .settings(dto.settings())
                .role(dto.roleName())
                .build();
    }

    public RestaurantDomain toDomain(RestaurantUpdateRequest request, UUID id, UUID ownerId) {
        return RestaurantDomain.builder()
                .id(id)
                .ownerId(ownerId)
                .name(request.name())
                .address(request.address())
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .operationMode(request.operationMode() == null ? null : OperationMode.valueOf(request.operationMode()))
                .isActive(request.isActive())
                .settings(request.settings())
                .build();
    }
}
