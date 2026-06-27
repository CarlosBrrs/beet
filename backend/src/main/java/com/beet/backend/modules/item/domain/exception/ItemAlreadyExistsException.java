package com.beet.backend.modules.item.domain.exception;

public class ItemAlreadyExistsException extends RuntimeException {

    private ItemAlreadyExistsException(String message) {
        super(message);
    }

    public static ItemAlreadyExistsException forName(String name) {
        return new ItemAlreadyExistsException("An item with this name already exists: " + name);
    }
}
