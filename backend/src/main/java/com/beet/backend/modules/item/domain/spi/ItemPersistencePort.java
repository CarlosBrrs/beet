package com.beet.backend.modules.item.domain.spi;

import com.beet.backend.modules.item.domain.model.ItemClass;
import com.beet.backend.modules.item.domain.model.ItemDomain;
import com.beet.backend.modules.item.domain.model.RecipeLineDomain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemPersistencePort {

    ItemDomain save(ItemDomain item);

    ItemDomain update(ItemDomain item);

    Optional<ItemDomain> findById(UUID id);

    List<ItemDomain> findAllByRestaurantAndClass(UUID restaurantId, ItemClass itemClass);

    boolean existsByNameAndRestaurant(String name, UUID restaurantId);

    void deleteById(UUID id);

    // Recipe lines management
    RecipeLineDomain saveRecipeLine(RecipeLineDomain line);

    void deleteRecipeLinesByParent(UUID parentItemId);

    List<RecipeLineDomain> findRecipeLinesByParent(UUID parentItemId);

    // For circular reference detection — returns all child item IDs referencing the
    // given parent chain
    List<UUID> findAllPreparationDescendantIds(UUID itemId);

    // For cost calculation: retrieve the base_unit factor for a given unit
    // Returns factor_to_base (1 if it's already a base unit)
    java.math.BigDecimal getUnitFactorToBase(UUID unitId);

    java.util.Optional<UUID> findUnitIdByAbbreviation(String abbreviation);

    // For cost calculation: retrieve last_cost_base for a master_ingredient
    java.math.BigDecimal getIngredientLastCostBase(UUID masterIngredientId);

    // Create a submenu_nodes entry linking a SALEABLE_PRODUCT to a submenu
    void saveSubmenuNode(UUID submenuId, UUID itemId);

    // Get all items (SALEABLE_PRODUCT) linked to a submenu via submenu_nodes
    List<ItemDomain> findItemsBySubmenu(UUID submenuId);
}
