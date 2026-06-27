package com.beet.backend.modules.item.domain.model;

public enum ItemClass {
    PREPARATION, // Internal sub-recipe — not sold directly
    PRODUCT // Sold to customers — flat or with recipe
}
