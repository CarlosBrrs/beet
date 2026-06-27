package com.beet.backend.modules.cash.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceNotFoundException;

import java.util.UUID;

public class CashSessionNotFoundException extends ResourceNotFoundException {
    private static final String MESSAGE_TEMPLATE = "Cash session not found: %s";
    private static final String ACTIVE_TEMPLATE = "No active cash session for device %s";

    private CashSessionNotFoundException(String message) {
        super(message);
    }

    public static CashSessionNotFoundException forId(UUID id) {
        return new CashSessionNotFoundException(String.format(MESSAGE_TEMPLATE, id));
    }

    public static CashSessionNotFoundException forDevice(UUID deviceId) {
        return new CashSessionNotFoundException(String.format(ACTIVE_TEMPLATE, deviceId));
    }
}
