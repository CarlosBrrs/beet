package com.beet.backend.modules.item.domain.api;

import com.beet.backend.modules.item.domain.model.ItemClass;
import com.beet.backend.modules.item.domain.model.ItemDomain;
import com.beet.backend.modules.item.domain.model.ProductDependenciesDomain;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.util.List;
import java.util.UUID;

public interface ItemServicePort {

    /** Create a new PREPARATION (sub-recipe). Always inventory-tracked. */
    ItemDomain createPreparation(ItemDomain item);

    /** Create a new PRODUCT, either tracked (with recipe) or flat. */
    ItemDomain createProduct(UUID submenuId, ItemDomain item);

    /** Update an existing item (preparation or product). */
    ItemDomain updateItem(ItemDomain item);

    ItemDomain updateItem(UUID restaurantId, ItemDomain item);

    /** Soft-delete an item by id. */
    void deleteItem(UUID id);

    void deleteItem(UUID restaurantId, UUID id);

    /** Retrieve an item with its recipe lines. */
    ItemDomain getById(UUID id);

    ItemDomain getById(UUID restaurantId, UUID id);

    /** Get all items of a given class for a tenant owner. */
    List<ItemDomain> getAllByRestaurantAndClass(UUID restaurantId, ItemClass itemClass);

    PageResponse<ItemDomain> getProductsPaged(UUID restaurantId, int page, int size, String search);

    ItemDomain createProduct(ItemDomain item);

    List<ItemDomain> getTemplateOptions(UUID restaurantId);

    ItemDomain setProductActive(UUID restaurantId, UUID itemId, boolean active, UUID userId);

    ProductDependenciesDomain getProductDependencies(UUID restaurantId, UUID itemId);
}
