package com.beet.backend.modules.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PosTemplateOptionDomain {
    private UUID slotOptionId;
    private UUID itemId;
    private String itemName;
    private BigDecimal surcharge;
    private int maxQuantity;
    private boolean isDefault;
    private boolean available;
    private boolean lowStock;
    private String unavailableReason;
    private int sortOrder;

    @Builder.Default
    private List<String> insufficientIngredients = new ArrayList<>();
}
