package com.beet.backend.modules.restaurant.infrastructure.output.persistence.jdbc.mapper;

import com.beet.backend.modules.restaurant.domain.model.RestaurantDomain;
import com.beet.backend.modules.restaurant.infrastructure.output.persistence.jdbc.aggregate.RestaurantAggregate;

import org.springframework.stereotype.Component;

@Component
public class RestaurantAggregateMapper {

    public RestaurantDomain toDomain(RestaurantAggregate aggregate) {
        if (aggregate == null)
            return null;
        return RestaurantDomain.builder()
                .id(aggregate.getId())
                .name(aggregate.getName())
                .address(aggregate.getAddress())
                .email(aggregate.getEmail())
                .phoneNumber(aggregate.getPhoneNumber())
                .operationMode(aggregate.getOperationMode())
                .isActive(aggregate.getIsActive())
                .ownerId(aggregate.getOwnerId())
                .settings(aggregate.getSettings())
                .build();
    }

    public RestaurantAggregate toAggregate(RestaurantDomain domain) {
        if (domain == null)
            return null;
        return RestaurantAggregate.builder()
                .id(domain.getId())
                .name(domain.getName())
                .address(domain.getAddress())
                .email(domain.getEmail())
                .phoneNumber(domain.getPhoneNumber())
                .operationMode(domain.getOperationMode())
                .isActive(domain.getIsActive())
                .ownerId(domain.getOwnerId())
                .settings(domain.getSettings())
                .build();
    }

    public void updateAggregate(RestaurantAggregate aggregate, RestaurantDomain domain) {
        aggregate.setName(domain.getName());
        aggregate.setAddress(domain.getAddress());
        aggregate.setEmail(domain.getEmail());
        aggregate.setPhoneNumber(domain.getPhoneNumber());
        aggregate.setOperationMode(domain.getOperationMode());
        aggregate.setIsActive(domain.getIsActive());
        aggregate.setSettings(domain.getSettings());
        // ownerId should not change usually, but we can set it
        aggregate.setOwnerId(domain.getOwnerId());
    }
}
