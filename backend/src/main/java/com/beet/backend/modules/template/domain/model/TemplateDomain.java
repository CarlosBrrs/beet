package com.beet.backend.modules.template.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Layer 3 — Template/Combo.
 * A template groups multiple slots with options; the final price =
 * base_price + SUM of surcharges from the customer's chosen options.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class TemplateDomain {

    private UUID id;
    private UUID restaurantId;
    private String name;
    private String description;

    /** Starting price charged to the customer before any surcharges. */
    private BigDecimal basePrice;
    private boolean isActive;
    private boolean isPublished;
    private UUID createdBy;
    private UUID updatedBy;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @Builder.Default
    private List<TemplateSlotDomain> slots = new ArrayList<>();
}
