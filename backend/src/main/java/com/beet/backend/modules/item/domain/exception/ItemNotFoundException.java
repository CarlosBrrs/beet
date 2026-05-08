package com.beet.backend.modules.item.domain.exception;

import java.util.UUID;

public class ItemNotFoundException extends RuntimeException {

    private ItemNotFoundException(String message) {
        super(message);
    }

    public static ItemNotFoundException forId(UUID id) {
        return new ItemNotFoundException("Item not found with id: " + id);
    }

    public static ItemNotFoundException forName(String name) {
        return new ItemNotFoundException("Item not found with name: " + name);
    }
}
