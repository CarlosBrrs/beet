package com.beet.backend.modules.menu.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceAlreadyExistsException;

public class MenuAlreadyExistsException extends ResourceAlreadyExistsException {
    private static final String MESSAGE_TEMPLATE = "Menu with name '%s' already exists in this restaurant";

    private MenuAlreadyExistsException(String message) {
        super(message);
    }

    public static MenuAlreadyExistsException forName(String name) {
        return new MenuAlreadyExistsException(String.format(MESSAGE_TEMPLATE, name));
    }
}
