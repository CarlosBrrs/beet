package com.beet.backend.modules.inventory.domain.exception;

/**
 * Thrown when an ingredient is already activated in a restaurant.
 */
public class IngredientAlreadyActivatedException extends RuntimeException {

    public IngredientAlreadyActivatedException(String message) {
        super(message);
    }

    public static IngredientAlreadyActivatedException forIngredient(String ingredientId, String restaurantId) {
        return new IngredientAlreadyActivatedException(
                String.format("Ingredient %s is already activated in restaurant %s", ingredientId, restaurantId));
    }
}
