package com.beet.backend.modules.item.domain.model;

public enum RecipeLineSource {
    INGREDIENT, // Points to master_ingredients
    PREPARATION // Points to another item (class = PREPARATION)
}
