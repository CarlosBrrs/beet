package com.beet.backend.modules.item.domain.usecase;

import com.beet.backend.modules.item.domain.api.ItemServicePort;
import com.beet.backend.modules.item.domain.exception.ItemAlreadyExistsException;
import com.beet.backend.modules.item.domain.exception.ItemNotFoundException;
import com.beet.backend.modules.item.domain.exception.ItemValidationException;
import com.beet.backend.modules.item.domain.model.ItemClass;
import com.beet.backend.modules.item.domain.model.ItemDomain;
import com.beet.backend.modules.item.domain.model.RecipeLineDomain;
import com.beet.backend.modules.item.domain.model.RecipeLineSource;
import com.beet.backend.modules.item.domain.spi.ItemPersistencePort;
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
        BigDecimal cost = calculateBomCost(saved);
        saved.setTheoreticalCost(cost);
        return itemPersistencePort.update(saved);
    }

    @Override
    @Transactional
    public ItemDomain createProduct(UUID submenuId, ItemDomain item) {
        guardDuplicateName(item.getName(), item.getRestaurantId());
        prepareProductForCreate(item);

        ItemDomain saved = itemPersistencePort.save(item);
        itemPersistencePort.saveSubmenuNode(submenuId, saved.getId());

        if (item.isInventoryTracked() && !item.getRecipeLines().isEmpty()) {
            saveRecipeLines(saved.getId(), item.getRecipeLines());
            saved.setRecipeLines(itemPersistencePort.findRecipeLinesByParent(saved.getId()));
            BigDecimal cost = calculateBomCost(saved);
            saved.setTheoreticalCost(cost);
            return itemPersistencePort.update(saved);
        }
        return saved;
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
        existing.setSalePrice(item.getSalePrice());

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
            existing.setTheoreticalCost(calculateBomCost(existing));
        }

        return itemPersistencePort.update(existing);
    }

    @Override
    @Transactional
    public void deleteItem(UUID id) {
        itemPersistencePort.findById(id)
                .orElseThrow(() -> ItemNotFoundException.forId(id));
        itemPersistencePort.deleteById(id);
    }

    @Override
    public ItemDomain getById(UUID id) {
        ItemDomain item = itemPersistencePort.findById(id)
                .orElseThrow(() -> ItemNotFoundException.forId(id));
        item.setRecipeLines(itemPersistencePort.findRecipeLinesByParent(id));
        return item;
    }

    @Override
    public List<ItemDomain> getAllByRestaurantAndClass(UUID restaurantId, ItemClass itemClass) {
        List<ItemDomain> items = itemPersistencePort.findAllByRestaurantAndClass(restaurantId, itemClass);
        items.forEach(item -> item.setRecipeLines(itemPersistencePort.findRecipeLinesByParent(item.getId())));
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

    private void prepareProductForCreate(ItemDomain item) {
        if (item.isInventoryTracked()) {
            if (item.getRecipeLines() == null || item.getRecipeLines().isEmpty()) {
                throw ItemValidationException.trackedProductRequiresRecipe();
            }
            if (item.getYieldQty() == null || item.getYieldUnitId() == null) {
                throw ItemValidationException.trackedProductRequiresYield();
            }
            validateNoCycles(item.getRecipeLines(), null);
            return;
        }

        if (item.getRecipeLines() != null && !item.getRecipeLines().isEmpty()) {
            throw ItemValidationException.flatProductCannotHaveRecipe();
        }
        item.setRecipeLines(List.of());
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
    private BigDecimal calculateBomCost(ItemDomain item) {
        if (item.getRecipeLines().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalRawCost = BigDecimal.ZERO;

        for (RecipeLineDomain line : item.getRecipeLines()) {
            BigDecimal lineCost = calculateLineCost(line);
            totalRawCost = totalRawCost.add(lineCost);
        }

        // Divide by yield to get cost per unit of output
        BigDecimal yieldInBase = toBase(item.getYieldQty(), item.getYieldUnitId());
        if (yieldInBase.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalRawCost.divide(yieldInBase, SCALE, RoundingMode.HALF_UP);
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
