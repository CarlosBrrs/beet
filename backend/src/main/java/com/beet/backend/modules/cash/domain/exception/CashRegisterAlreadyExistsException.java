package com.beet.backend.modules.cash.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceAlreadyExistsException;

public class CashRegisterAlreadyExistsException extends ResourceAlreadyExistsException {
    private static final String MESSAGE_TEMPLATE = "Cash register with name '%s' already exists";
    private static final String DEVICE_TEMPLATE = "Cash register already assigned to device %s";

    private CashRegisterAlreadyExistsException(String message) {
        super(message);
    }

    public static CashRegisterAlreadyExistsException forName(String name) {
        return new CashRegisterAlreadyExistsException(String.format(MESSAGE_TEMPLATE, name));
    }

    public static CashRegisterAlreadyExistsException forDevice(String deviceId) {
        return new CashRegisterAlreadyExistsException(String.format(DEVICE_TEMPLATE, deviceId));
    }
}
