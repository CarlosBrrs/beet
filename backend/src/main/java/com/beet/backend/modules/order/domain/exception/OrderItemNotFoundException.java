package com.beet.backend.modules.order.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceNotFoundException;

import java.util.UUID;

public class OrderItemNotFoundException extends ResourceNotFoundException {
    private static final String MESSAGE_TEMPLATE = "Order item not found with id: %s";

    private OrderItemNotFoundException(String message) {
        super(message);
    }

    public static OrderItemNotFoundException forId(UUID id) {
        return new OrderItemNotFoundException(String.format(MESSAGE_TEMPLATE, id));
    }
}
