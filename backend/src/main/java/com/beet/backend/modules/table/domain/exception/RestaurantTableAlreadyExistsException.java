package com.beet.backend.modules.table.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceAlreadyExistsException;

public class RestaurantTableAlreadyExistsException extends ResourceAlreadyExistsException {
    private RestaurantTableAlreadyExistsException(String message) {
        super(message);
    }

    public static RestaurantTableAlreadyExistsException forName(String name) {
        return new RestaurantTableAlreadyExistsException("Restaurant table already exists with name: " + name);
    }
}
