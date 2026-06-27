package com.beet.backend.modules.inventory.domain.exception;

/**
 * Thrown when an ingredient stock entry is not found.
 */
public class IngredientStockNotFoundException extends RuntimeException {

    public IngredientStockNotFoundException(String message) {
        super(message);
    }

    public static IngredientStockNotFoundException forId(String stockId) {
        return new IngredientStockNotFoundException(
                String.format("Ingredient stock not found: %s", stockId));
    }
}
