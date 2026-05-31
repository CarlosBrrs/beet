package com.beet.backend.modules.cash.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceAlreadyExistsException;

public class CashSessionConflictException extends ResourceAlreadyExistsException {
    private static final String OPEN_REGISTER_TEMPLATE = "Cash register already has an open session";
    private static final String OPEN_DEVICE_TEMPLATE = "Device already has an open session";
    private static final String ALREADY_CLOSED_TEMPLATE = "Cash session is already closed";

    private CashSessionConflictException(String message) {
        super(message);
    }

    public static CashSessionConflictException openRegister() {
        return new CashSessionConflictException(OPEN_REGISTER_TEMPLATE);
    }

    public static CashSessionConflictException openDevice() {
        return new CashSessionConflictException(OPEN_DEVICE_TEMPLATE);
    }

    public static CashSessionConflictException alreadyClosed() {
        return new CashSessionConflictException(ALREADY_CLOSED_TEMPLATE);
    }
}
