package com.beet.backend.modules.restaurant.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceAlreadyExistsException;

public class RestaurantAlreadyExistsException extends ResourceAlreadyExistsException {

    private static final String ALREADY_EXISTS_TEMPLATE = "Restaurant with %s already exists: %s";

    private RestaurantAlreadyExistsException(String message) {
        super(message);
    }

    public static RestaurantAlreadyExistsException forField(String fieldName, String value) {
        return new RestaurantAlreadyExistsException(String.format(ALREADY_EXISTS_TEMPLATE, fieldName, value));
    }

}
