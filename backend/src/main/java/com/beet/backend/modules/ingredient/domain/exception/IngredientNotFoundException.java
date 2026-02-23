package com.beet.backend.modules.ingredient.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceNotFoundException;

import java.util.UUID;

public class IngredientNotFoundException extends ResourceNotFoundException {

    public IngredientNotFoundException(String message) {
        super(message);
    }

    public static IngredientNotFoundException forId(UUID id) {
        return new IngredientNotFoundException(
                "Ingredient with id '" + id + "' was not found");
    }
}
