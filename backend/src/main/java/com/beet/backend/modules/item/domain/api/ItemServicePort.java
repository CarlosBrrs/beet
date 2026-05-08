package com.beet.backend.modules.item.domain.api;

import com.beet.backend.modules.item.domain.model.ItemClass;
import com.beet.backend.modules.item.domain.model.ItemDomain;

import java.util.List;
import java.util.UUID;

public interface ItemServicePort {

    /** Create a new PREPARATION (sub-recipe). Always inventory-tracked. */
    ItemDomain createPreparation(ItemDomain item);

    /** Create a new SALEABLE_PRODUCT, either tracked (with recipe) or flat. */
    ItemDomain createProduct(UUID submenuId, ItemDomain item);

    /** Update an existing item (preparation or product). */
    ItemDomain updateItem(ItemDomain item);

    /** Soft-delete an item by id. */
    void deleteItem(UUID id);

    /** Retrieve an item with its recipe lines. */
    ItemDomain getById(UUID id);

    /** Get all items of a given class for a tenant owner. */
    List<ItemDomain> getAllByRestaurantAndClass(UUID restaurantId, ItemClass itemClass);
}
