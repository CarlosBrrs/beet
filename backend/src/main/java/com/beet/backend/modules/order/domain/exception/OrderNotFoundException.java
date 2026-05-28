package com.beet.backend.modules.order.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceNotFoundException;

import java.util.UUID;

public class OrderNotFoundException extends ResourceNotFoundException {
    private static final String MESSAGE_TEMPLATE = "Order not found with id: %s";

    private OrderNotFoundException(String message) {
        super(message);
    }

    public static OrderNotFoundException forId(UUID id) {
        return new OrderNotFoundException(String.format(MESSAGE_TEMPLATE, id));
    }
}
