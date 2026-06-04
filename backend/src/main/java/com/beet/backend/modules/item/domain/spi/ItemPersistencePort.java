package com.beet.backend.modules.item.domain.spi;

import com.beet.backend.modules.item.domain.model.ItemClass;
import com.beet.backend.modules.item.domain.model.ItemDomain;
import com.beet.backend.modules.item.domain.model.RecipeLineDomain;
import com.beet.backend.modules.item.domain.model.ProductDependenciesDomain;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemPersistencePort {

    ItemDomain save(ItemDomain item);

    ItemDomain update(ItemDomain item);

    Optional<ItemDomain> findById(UUID id);

    List<ItemDomain> findAllByRestaurantAndClass(UUID restaurantId, ItemClass itemClass);

    PageResponse<ItemDomain> findAllByRestaurantAndClassPaged(
            UUID restaurantId, ItemClass itemClass, int page, int size, String search);

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

    // Create a submenu_nodes entry linking a PRODUCT to a submenu
    void saveSubmenuNode(UUID submenuId, UUID itemId);

    // Get all items (PRODUCT) linked to a submenu via submenu_nodes
    List<ItemDomain> findItemsBySubmenu(UUID submenuId);

    List<ItemDomain> findTemplateOptions(UUID restaurantId);

    void updateActivation(UUID restaurantId, UUID itemId, boolean isActive, UUID userId);

    boolean isPublished(UUID restaurantId, UUID itemId);

    boolean isUsedAsTemplateOption(UUID restaurantId, UUID itemId);

    ProductDependenciesDomain findDependencies(UUID restaurantId, UUID itemId);
}
