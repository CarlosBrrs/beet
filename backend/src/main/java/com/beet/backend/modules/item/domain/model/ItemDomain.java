package com.beet.backend.modules.item.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ItemDomain {

    private UUID id;
    private UUID restaurantId;
    private ItemClass itemClass;
    private String name;
    private String description;

    // Inventory tracking
    private boolean isInventoryTracked;

    // Yield — stored as entered by the user, normalized at calculation time
    private BigDecimal yieldQty;
    private UUID yieldUnitId;

    // Pricing
    private BigDecimal salePrice; // Set by user (SALEABLE_PRODUCT only)
    private BigDecimal theoreticalCost; // Calculated by system (or entered if flat)

    // Audit
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy; // NEW: To satisfy items table foreign key
    private UUID updatedBy; // NEW: To satisfy items table foreign key

    // BOM lines (loaded with item when needed)
    @Builder.Default
    private List<RecipeLineDomain> recipeLines = new ArrayList<>();
}
