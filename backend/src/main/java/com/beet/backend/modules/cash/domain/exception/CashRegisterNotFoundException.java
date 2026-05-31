package com.beet.backend.modules.cash.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceNotFoundException;

import java.util.UUID;

public class CashRegisterNotFoundException extends ResourceNotFoundException {
    private static final String MESSAGE_TEMPLATE = "Cash register not found: %s";

    private CashRegisterNotFoundException(String message) {
        super(message);
    }

    public static CashRegisterNotFoundException forId(UUID id) {
        return new CashRegisterNotFoundException(String.format(MESSAGE_TEMPLATE, id));
    }
}
