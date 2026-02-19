package com.beet.backend.modules.restaurant.domain.model;

import lombok.Builder;

// Using Record for immutability and simplicity as it's a Value Object
@Builder
public record RestaurantSettings(
        Boolean prePaymentEnabled,
        Boolean allowTakeaway,
        Boolean allowDelivery,
        Integer maxTableCapacity) {
}
