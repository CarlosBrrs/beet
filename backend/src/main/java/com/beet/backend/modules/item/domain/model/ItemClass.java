package com.beet.backend.modules.item.domain.model;

public enum ItemClass {
    PREPARATION, // Internal sub-recipe — not sold directly
    SALEABLE_PRODUCT // Sold to customers — flat or with recipe
}
