package com.beet.backend.modules.template.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A specific choice within a slot (e.g. "Burger Doble").
 * Only PRODUCT items can be options.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class SlotOptionDomain {

    private UUID id;
    private UUID slotId;

    /** Must reference an item with class = PRODUCT */
    private UUID itemId;

    /** Extra cost added to the template's base_price when this option is chosen. */
    private BigDecimal surcharge;

    private boolean isDefault;
    private int maxQuantity;
    private int sortOrder;
}
