package com.beet.backend.modules.restaurant.domain.model;

import lombok.Builder;

import java.math.BigDecimal;

// Using Record for immutability and simplicity as it's a Value Object
@Builder
public record RestaurantSettings(
                Boolean prePaymentEnabled,
                Boolean allowTakeaway,
                Boolean allowDelivery,
                Integer maxTableCapacity,
                TaxApplyMode taxApplyMode, // PER_INVOICE or PER_ITEM
                BigDecimal defaultTaxPercentage) { // Default: 19.00 for Colombia

        public enum TaxApplyMode {
                PER_INVOICE, // Single tax % on the invoice header
                PER_ITEM // Each item has its own tax %
        }
}
