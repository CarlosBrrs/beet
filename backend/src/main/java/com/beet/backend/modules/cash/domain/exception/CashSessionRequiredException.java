package com.beet.backend.modules.cash.domain.exception;

import java.util.UUID;

public class CashSessionRequiredException extends RuntimeException {

    private CashSessionRequiredException(String message) {
        super(message);
    }

    public static CashSessionRequiredException forDevice(UUID deviceId) {
        return new CashSessionRequiredException(
                String.format("An open cash session is required for device %s", deviceId));
    }
}
