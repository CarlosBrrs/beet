package com.beet.backend.modules.template.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TemplateResponse(
                UUID id,
                UUID restaurantId,
                String name,
                String description,
                BigDecimal basePrice,
                List<SlotResponse> slots,
                OffsetDateTime createdAt,
                OffsetDateTime updatedAt) {
        public record SlotResponse(
                        UUID id,
                        String name,
                        int minSelection,
                        int maxSelection,
                        int sortOrder,
                        List<SlotOptionResponse> options) {
        }

        public record SlotOptionResponse(
                        UUID id,
                        UUID itemId,
                        BigDecimal surcharge,
                        boolean isDefault,
                        int sortOrder) {
        }
}
