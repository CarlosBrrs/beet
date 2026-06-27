package com.beet.backend.modules.menu.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceNotFoundException;

import java.util.UUID;

public class MenuNotFoundException extends ResourceNotFoundException {
    private static final String MESSAGE_TEMPLATE = "Menu with ID %s not found";

    private MenuNotFoundException(String message) {
        super(message);
    }

    public static MenuNotFoundException forId(UUID id) {
        return new MenuNotFoundException(String.format(MESSAGE_TEMPLATE, id));
    }
}
