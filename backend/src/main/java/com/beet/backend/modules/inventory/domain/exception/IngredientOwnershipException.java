package com.beet.backend.modules.inventory.domain.exception;

/**
 * Thrown when an ingredient does not belong to the authenticated owner.
 */
public class IngredientOwnershipException extends RuntimeException {

    public IngredientOwnershipException(String message) {
        super(message);
    }

    public static IngredientOwnershipException forIngredient(String ingredientId) {
        return new IngredientOwnershipException(
                String.format("Ingredient %s does not belong to the authenticated owner", ingredientId));
    }
}
