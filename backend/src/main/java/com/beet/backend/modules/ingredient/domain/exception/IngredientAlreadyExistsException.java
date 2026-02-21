package com.beet.backend.modules.ingredient.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceAlreadyExistsException;

public class IngredientAlreadyExistsException extends ResourceAlreadyExistsException {

    public IngredientAlreadyExistsException(String message) {
        super(message);
    }

    public static IngredientAlreadyExistsException forName(String name) {
        return new IngredientAlreadyExistsException(
                "An ingredient with name '" + name + "' already exists for this owner");
    }
}
