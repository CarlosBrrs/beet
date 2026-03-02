package com.beet.backend.modules.menu.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceNotFoundException;

import java.util.UUID;

public class SubmenuNotFoundException extends ResourceNotFoundException {
    private static final String MESSAGE_TEMPLATE = "Submenu with ID %s not found";

    private SubmenuNotFoundException(String message) {
        super(message);
    }

    public static SubmenuNotFoundException forId(UUID id) {
        return new SubmenuNotFoundException(String.format(MESSAGE_TEMPLATE, id));
    }
}
