package com.beet.backend.modules.ingredient.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Flat read model for the ingredient list table view.
 * Kept minimal — only what the list UI consumes.
 */
public record IngredientListResponse(
        UUID id,
        String name,
        String unitAbbreviation, // units.abbreviation e.g. "kg"
        BigDecimal costPerBaseUnit // supplier_items.last_cost_base; null if no active supplier yet
) {
}
