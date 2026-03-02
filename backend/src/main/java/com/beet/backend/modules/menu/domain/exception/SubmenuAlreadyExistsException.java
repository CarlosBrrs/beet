package com.beet.backend.modules.menu.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceAlreadyExistsException;

public class SubmenuAlreadyExistsException extends ResourceAlreadyExistsException {
    private static final String MESSAGE_TEMPLATE = "Submenu with name '%s' already exists in this menu";

    private SubmenuAlreadyExistsException(String message) {
        super(message);
    }

    public static SubmenuAlreadyExistsException forName(String name) {
        return new SubmenuAlreadyExistsException(String.format(MESSAGE_TEMPLATE, name));
    }
}
