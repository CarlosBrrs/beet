package com.beet.backend.modules.item.domain.usecase;

import com.beet.backend.modules.item.domain.api.ItemServicePort;
import com.beet.backend.modules.item.domain.api.RecipeCalculationServicePort;
import com.beet.backend.modules.item.domain.exception.ItemAlreadyExistsException;
import com.beet.backend.modules.item.domain.exception.ItemNotFoundException;
import com.beet.backend.modules.item.domain.exception.ItemValidationException;
import com.beet.backend.modules.item.domain.model.ItemClass;
import com.beet.backend.modules.item.domain.model.ItemDomain;
import com.beet.backend.modules.item.domain.model.ProductDependenciesDomain;
import com.beet.backend.modules.item.domain.model.RecipeLineDomain;
import com.beet.backend.modules.item.domain.model.RecipeLineSource;
import com.beet.backend.modules.item.domain.spi.ItemPersistencePort;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemUseCase implements ItemServicePort {

    private static final int SCALE = 6;

    private final ItemPersistencePort itemPersistencePort;
    private final RecipeCalculationServicePort recipeCalculationService;

    // --------------------------------------------------------------------------
    // Public use cases
    // --------------------------------------------------------------------------

    @Override
    @Transactional
    public ItemDomain createPreparation(ItemDomain item) {
        guardDuplicateName(item.getName(), item.getRestaurantId());
        validateNoCycles(item.getRecipeLines(), null);
        ItemDomain saved = itemPersistencePort.save(item);
        saveRecipeLines(saved.getId(), item.getRecipeLines());
        saved.setRecipeLines(itemPersistencePort.findRecipeLinesByParent(saved.getId()));
        recipeCalculationService.recalculate(saved.getRestaurantId(), saved.getId(), saved.getUpdatedBy());
        return getById(saved.getRestaurantId(), saved.getId());
    }

    @Override
    @Transactional
    public ItemDomain createProduct(UUID submenuId, ItemDomain item) {
        if (item.getSalePrice() == null || item.getSalePrice().signum() <= 0) {
            throw new IllegalArgumentException("Published products require a sale price greater than zero.");
        }
        ItemDomain saved = createProduct(item);
        itemPersistencePort.saveSubmenuNode(submenuId, saved.getId());
        saved.setPublished(true);
        return saved;
    }

    @Override
    @Transactional
    public ItemDomain createProduct(ItemDomain item) {
        guardDuplicateName(item.getName(), item.getRestaurantId());
        prepareProductForCreate(item);

        ItemDomain saved = itemPersistencePort.save(item);

        if (item.isInventoryTracked() && !item.getRecipeLines().isEmpty()) {
            saveRecipeLines(saved.getId(), item.getRecipeLines());
            saved.setRecipeLines(itemPersistencePort.findRecipeLinesByParent(saved.getId()));
            recipeCalculationService.recalculate(saved.getRestaurantId(), saved.getId(), saved.getUpdatedBy());
            return getById(saved.getRestaurantId(), saved.getId());
        }
        return saved;
    }

    @Override
    public List<ItemDomain> getTemplateOptions(UUID restaurantId) {
        List<ItemDomain> options = itemPersistencePort.findTemplateOptions(restaurantId);
        options.forEach(this::decorateCatalogState);
        return options;
    }

    @Override
    @Transactional
    public ItemDomain setProductActive(UUID restaurantId, UUID itemId, boolean active, UUID userId) {
        ItemDomain item = itemPersistencePort.findById(itemId)
                .filter(found -> restaurantId.equals(found.getRestaurantId()))
                .orElseThrow(() -> ItemNotFoundException.forId(itemId));
        if (!active && (itemPersistencePort.isPublished(restaurantId, itemId)
                || itemPersistencePort.isUsedAsTemplateOption(restaurantId, itemId))) {
            throw new IllegalArgumentException("Remove product publications and template usages before deactivating it.");
        }
        itemPersistencePort.updateActivation(restaurantId, itemId, active, userId);
        item.setActive(active);
        return decorateCatalogState(item);
    }

    @Override
    @Transactional
    public ItemDomain updateItem(ItemDomain item) {
        ItemDomain existing = itemPersistencePort.findById(item.getId())
                .orElseThrow(() -> ItemNotFoundException.forId(item.getId()));

        if (!existing.getName().equalsIgnoreCase(item.getName()) &&
                itemPersistencePort.existsByNameAndRestaurant(item.getName(), existing.getRestaurantId())) {
            throw ItemAlreadyExistsException.forName(item.getName());
        }

        // Merge fields
        existing.setName(item.getName());
        existing.setDescription(item.getDescription());
        existing.setYieldQty(item.getYieldQty());
        existing.setYieldUnitId(item.getYieldUnitId());
        existing.setSellableUnitsPerBatch(item.getSellableUnitsPerBatch());
        existing.setSalePrice(item.getSalePrice());
        if (existing.getItemClass() == ItemClass.PRODUCT
                && itemPersistencePort.isPublished(existing.getRestaurantId(), existing.getId())
                && (item.getSalePrice() == null || item.getSalePrice().signum() <= 0)) {
            throw new IllegalArgumentException("Published products require a sale price greater than zero.");
        }
        if (existing.isAvailableAsTemplateOption() && !item.isAvailableAsTemplateOption()
                && itemPersistencePort.isUsedAsTemplateOption(existing.getRestaurantId(), existing.getId())) {
            throw new IllegalArgumentException("Remove the product from template slots before disabling template eligibility.");
        }
        existing.setAvailableAsTemplateOption(item.isAvailableAsTemplateOption());
        existing.setUpdatedBy(item.getUpdatedBy());

        // Replace recipe lines
        itemPersistencePort.deleteRecipeLinesByParent(existing.getId());
        if (item.isInventoryTracked() && !item.getRecipeLines().isEmpty()) {
            validateNoCycles(item.getRecipeLines(), existing.getId());
            saveRecipeLines(existing.getId(), item.getRecipeLines());
        }
        existing.setRecipeLines(itemPersistencePort.findRecipeLinesByParent(existing.getId()));

        // Flat product: user provides cost directly
        if (!existing.isInventoryTracked()) {
            existing.setTheoreticalCost(item.getTheoreticalCost());
        } else {
            existing.setTheoreticalCost(null);
        }

        ItemDomain updated = itemPersistencePort.update(existing);
        if (updated.isInventoryTracked()) {
            recipeCalculationService.recalculate(
                    updated.getRestaurantId(), updated.getId(), updated.getUpdatedBy());
            updated = getById(updated.getRestaurantId(), updated.getId());
        }
        return decorateCatalogState(updated);
    }

    @Override
    @Transactional
    public ItemDomain updateItem(UUID restaurantId, ItemDomain item) {
        getById(restaurantId, item.getId());
        return updateItem(item);
    }

    @Override
    @Transactional
    public void deleteItem(UUID id) {
        itemPersistencePort.findById(id)
                .orElseThrow(() -> ItemNotFoundException.forId(id));
        itemPersistencePort.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteItem(UUID restaurantId, UUID id) {
        getById(restaurantId, id);
        deleteItem(id);
    }

    @Override
    public ItemDomain getById(UUID id) {
        ItemDomain item = itemPersistencePort.findById(id)
                .orElseThrow(() -> ItemNotFoundException.forId(id));
        item.setRecipeLines(itemPersistencePort.findRecipeLinesByParent(id));
        decorateRecipeCalculation(item);
        return decorateCatalogState(item);
    }

    @Override
    public ItemDomain getById(UUID restaurantId, UUID id) {
        ItemDomain item = getById(id);
        if (!restaurantId.equals(item.getRestaurantId())) {
            throw ItemNotFoundException.forId(id);
        }
        return item;
    }

    @Override
    public List<ItemDomain> getAllByRestaurantAndClass(UUID restaurantId, ItemClass itemClass) {
        List<ItemDomain> items = itemPersistencePort.findAllByRestaurantAndClass(restaurantId, itemClass);
        items.forEach(item -> {
            item.setRecipeLines(itemPersistencePort.findRecipeLinesByParent(item.getId()));
            decorateRecipeCalculation(item);
            decorateCatalogState(item);
        });
        return items;
    }

    // --------------------------------------------------------------------------
    // Private helpers
    // --------------------------------------------------------------------------

    private void guardDuplicateName(String name, UUID restaurantId) {
        if (itemPersistencePort.existsByNameAndRestaurant(name, restaurantId)) {
            throw ItemAlreadyExistsException.forName(name);
        }
    }

    @Override
    public PageResponse<ItemDomain> getProductsPaged(UUID restaurantId, int page, int size, String search) {
        validatePagination(page, size);
        PageResponse<ItemDomain> products = itemPersistencePort.findAllByRestaurantAndClassPaged(
                restaurantId, ItemClass.PRODUCT, page, size, search);
        products.content().forEach(this::decorateCatalogState);
        products.content().forEach(this::decorateRecipeCalculation);
        return products;
    }

    @Override
    public ProductDependenciesDomain getProductDependencies(UUID restaurantId, UUID itemId) {
        ItemDomain item = getById(restaurantId, itemId);
        if (item.getItemClass() != ItemClass.PRODUCT) {
            throw ItemNotFoundException.forId(itemId);
        }
        return itemPersistencePort.findDependencies(restaurantId, itemId);
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw new IllegalArgumentException("Pagination requires page >= 0 and size between 1 and 200.");
        }
    }

    private ItemDomain decorateCatalogState(ItemDomain item) {
        if (item.getItemClass() == ItemClass.PRODUCT) {
            item.setPublished(itemPersistencePort.isPublished(item.getRestaurantId(), item.getId()));
            item.setUsedAsTemplateOption(itemPersistencePort.isUsedAsTemplateOption(item.getRestaurantId(), item.getId()));
        }
        return item;
    }

    private void prepareProductForCreate(ItemDomain item) {
        if (item.isInventoryTracked()) {
            if (item.getRecipeLines() == null || item.getRecipeLines().isEmpty()) {
                throw ItemValidationException.trackedProductRequiresRecipe();
            }
            if (item.getYieldQty() == null || item.getYieldUnitId() == null) {
                throw ItemValidationException.trackedProductRequiresYield();
            }
            if (item.getSellableUnitsPerBatch() == null || item.getSellableUnitsPerBatch() < 1) {
                throw new IllegalArgumentException("Tracked products require at least one sellable unit per batch.");
            }
            validateNoCycles(item.getRecipeLines(), null);
            return;
        }

        if (item.getRecipeLines() != null && !item.getRecipeLines().isEmpty()) {
            throw ItemValidationException.flatProductCannotHaveRecipe();
        }
        item.setRecipeLines(List.of());
        item.setSellableUnitsPerBatch(1);
        if (item.getYieldQty() == null) {
            item.setYieldQty(BigDecimal.ONE);
        }
        if (item.getYieldUnitId() == null) {
            item.setYieldUnitId(itemPersistencePort.findUnitIdByAbbreviation("pcs")
                    .orElseThrow(() -> ItemValidationException.defaultUnitNotFound("pcs")));
        }
        if (item.getTheoreticalCost() == null) {
            item.setTheoreticalCost(BigDecimal.ZERO);
        }
    }

    private void saveRecipeLines(UUID parentId, List<RecipeLineDomain> lines) {
        for (int i = 0; i < lines.size(); i++) {
            RecipeLineDomain line = lines.get(i);
            line.setParentItemId(parentId);
            line.setSortOrder(i);
            itemPersistencePort.saveRecipeLine(line);
        }
    }

    /**
     * Recursive BOM cost calculation.
     *
     * For each line:
     * - INGREDIENT: normalise quantity to base unit, multiply by last_cost_base
     * - PREPARATION: compute proportional cost based on prep's yield and its own
     * cost
     *
     * Returns the theoretical cost for ONE unit of the item (i.e. one yield qty).
     */
    private void decorateRecipeCalculation(ItemDomain item) {
        if (!item.isInventoryTracked() || item.getId() == null) {
            item.setMissingCostIngredients(List.of());
            return;
        }
        var result = recipeCalculationService.calculate(item.getRestaurantId(), item.getId());
        item.setBatchTheoreticalCost(result.batchCost());
        item.setTheoreticalCost(result.costPerSellableUnit());
        item.setPortionUnitId(result.physicalBaseUnitId());
        item.setPortionUnitAbbreviation(result.physicalBaseUnitAbbreviation());
        item.setMissingCostIngredients(result.missingCostIngredients());
        if (item.getItemClass() == ItemClass.PRODUCT && item.getSellableUnitsPerBatch() != null) {
            item.setPortionSize(result.physicalYieldInBase().divide(
                    BigDecimal.valueOf(item.getSellableUnitsPerBatch()),
                    6,
                    java.math.RoundingMode.HALF_UP));
        }
    }

    private BigDecimal calculateBomCost(ItemDomain item) {
        return recipeCalculationService.calculate(item.getRestaurantId(), item.getId()).costPerSellableUnit();
    }

    /**
     * Returns the raw material cost for a single recipe line,
     * expressed in base units of the ingredient.
     */
    private BigDecimal calculateLineCost(RecipeLineDomain line) {
        if (line.getSource() == RecipeLineSource.INGREDIENT) {
            // qty_in_base × last_cost_base
            BigDecimal qtyInBase = toBase(line.getQuantity(), line.getUnitId());
            BigDecimal costPerBase = itemPersistencePort.getIngredientLastCostBase(line.getMasterIngredientId());
            return qtyInBase.multiply(costPerBase);

        } else {
            // PREPARATION: load child item, get its theoretical_cost (per base unit),
            // then multiply by the quantity requested in base units
            ItemDomain child = itemPersistencePort.findById(line.getChildItemId())
                    .orElseThrow(() -> ItemNotFoundException.forId(line.getChildItemId()));
            // Load recipe lines of child to ensure cost is deterministic
            child.setRecipeLines(itemPersistencePort.findRecipeLinesByParent(child.getId()));

            BigDecimal childCostPerBase;
            if (child.getTheoreticalCost() != null && child.getTheoreticalCost().compareTo(BigDecimal.ZERO) > 0) {
                childCostPerBase = child.getTheoreticalCost();
            } else {
                childCostPerBase = calculateBomCost(child);
            }

            BigDecimal qtyInBase = toBase(line.getQuantity(), line.getUnitId());
            return qtyInBase.multiply(childCostPerBase);
        }
    }

    /** Normalise a quantity to base units using the unit_conversions table. */
    private BigDecimal toBase(BigDecimal qty, UUID unitId) {
        BigDecimal factor = itemPersistencePort.getUnitFactorToBase(unitId);
        return qty.multiply(factor).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * DFS circular reference guard.
     * A cycle exists if any PREPARATION line, transitively, includes the
     * parentItemId.
     *
     * @param lines        Lines to validate for the item being saved.
     * @param parentItemId ID of the item being updated (null for new items).
     */
    private void validateNoCycles(List<RecipeLineDomain> lines, UUID parentItemId) {
        for (RecipeLineDomain line : lines) {
            if (line.getSource() == RecipeLineSource.PREPARATION && line.getChildItemId() != null) {
                if (line.getChildItemId().equals(parentItemId)) {
                    throw new IllegalArgumentException(
                            "Circular recipe reference detected: item cannot include itself.");
                }
                // Check all descendants of the child
                List<UUID> descendants = itemPersistencePort.findAllPreparationDescendantIds(line.getChildItemId());
                if (parentItemId != null && descendants.contains(parentItemId)) {
                    throw new IllegalArgumentException(
                            "Circular recipe reference detected: this preparation is already an ancestor of the target.");
                }
            }
        }
    }
}
