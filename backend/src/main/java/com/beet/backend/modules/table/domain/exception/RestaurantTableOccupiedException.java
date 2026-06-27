package com.beet.backend.modules.table.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceAlreadyExistsException;

import java.util.UUID;

public class RestaurantTableOccupiedException extends ResourceAlreadyExistsException {
    private RestaurantTableOccupiedException(String message) {
        super(message);
    }

    public static RestaurantTableOccupiedException forId(UUID tableId) {
        return new RestaurantTableOccupiedException("Restaurant table has an open order: " + tableId);
    }
}
