package com.beet.backend.modules.table.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceNotFoundException;

import java.util.UUID;

public class RestaurantTableNotFoundException extends ResourceNotFoundException {
    private RestaurantTableNotFoundException(String message) {
        super(message);
    }

    public static RestaurantTableNotFoundException forId(UUID id) {
        return new RestaurantTableNotFoundException("Restaurant table not found with id: " + id);
    }
}
