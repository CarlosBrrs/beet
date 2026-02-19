package com.beet.backend.modules.user.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceAlreadyExistsException;

public class UserAlreadyExistsException extends ResourceAlreadyExistsException {

    public static final String EMAlL_EXISTS_TEMPLATE = "Email already in use: %s";
    public static final String USERNAME_EXISTS_TEMPLATE = "Username already in use: %s";
    public static final String PHONE_EXISTS_TEMPLATE = "Phone number already in use: %s";

    private UserAlreadyExistsException(String template, String value) {
        super(String.format(template, value));
    }

    public static UserAlreadyExistsException forEmail(String email) {
        return new UserAlreadyExistsException(EMAlL_EXISTS_TEMPLATE, email);
    }

    public static UserAlreadyExistsException forUsername(String username) {
        return new UserAlreadyExistsException(USERNAME_EXISTS_TEMPLATE, username);
    }

    public static UserAlreadyExistsException forPhoneNumber(String phoneNumber) {
        return new UserAlreadyExistsException(PHONE_EXISTS_TEMPLATE, phoneNumber);
    }
}
