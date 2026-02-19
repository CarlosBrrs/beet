package com.beet.backend.modules.restaurant.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceNotFoundException;

import java.util.UUID;

public class RestaurantNotFoundException extends ResourceNotFoundException {

    private static final String ID_NOT_FOUND_TEMPLATE = "Restaurant not found with id: %s";

    private RestaurantNotFoundException(String message) {
        super(message);
    }

    public static RestaurantNotFoundException forId(UUID id) {
        return new RestaurantNotFoundException(String.format(ID_NOT_FOUND_TEMPLATE, id));
    }
}
