package com.beet.backend.modules.user.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceNotFoundException;

import java.util.UUID;

public class UserNotFoundException extends ResourceNotFoundException {

    private static final String TEMPLATE = "User not found with id: %s";
    public UserNotFoundException(String message) {
        super(String.format(TEMPLATE, message));
    }

    public static UserNotFoundException forId(UUID id) {
        return new UserNotFoundException(id.toString());
    }

    public static UserNotFoundException forUsername(String username) {
        return new UserNotFoundException(username);
    }
}
