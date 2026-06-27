package com.beet.backend.modules.item.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class RecipeLineDomain {

    private UUID id;
    private UUID parentItemId;

    private RecipeLineSource source;

    // Only one of these will be non-null
    private UUID masterIngredientId; // Source = INGREDIENT
    private UUID childItemId; // Source = PREPARATION

    // Stored in user-specified unit; normalized to base unit at calculation time
    private BigDecimal quantity;
    private UUID unitId;
    private String sourceName;
    private String unitName;
    private String unitAbbreviation;

    private int sortOrder;
}
