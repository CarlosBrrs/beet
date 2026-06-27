package com.beet.backend.modules.order.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddOrderItemsBatchRequest(
        @NotEmpty(message = "At least one item is required")
        List<@Valid AddOrderItemRequest> items) {
}
