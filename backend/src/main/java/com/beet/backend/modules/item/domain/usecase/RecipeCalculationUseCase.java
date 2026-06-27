package com.beet.backend.modules.item.domain.usecase;

import com.beet.backend.modules.item.domain.api.RecipeCalculationServicePort;
import com.beet.backend.modules.item.domain.exception.ItemNotFoundException;
import com.beet.backend.modules.item.domain.model.IngredientCostSource;
import com.beet.backend.modules.item.domain.model.ItemClass;
import com.beet.backend.modules.item.domain.model.ItemDomain;
import com.beet.backend.modules.item.domain.model.RecipeCalculationResult;
import com.beet.backend.modules.item.domain.model.RecipeIngredientRequirement;
import com.beet.backend.modules.item.domain.model.RecipeLineDomain;
import com.beet.backend.modules.item.domain.model.RecipeLineSource;
import com.beet.backend.modules.item.domain.model.UnitConversion;
import com.beet.backend.modules.item.domain.spi.ItemPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipeCalculationUseCase implements RecipeCalculationServicePort {

    private static final int SCALE = 6;
    private final ItemPersistencePort itemPersistence;

    @Override
    public RecipeCalculationResult calculate(UUID restaurantId, UUID itemId) {
        return calculateItem(restaurantId, itemId, new LinkedHashSet<>());
    }

    @Override
    @Transactional
    public RecipeCalculationResult recalculate(UUID restaurantId, UUID itemId, UUID userId) {
        RecipeCalculationResult result = calculate(restaurantId, itemId);
        itemPersistence.updateTheoreticalCost(
                itemId,
                result.costComplete() ? result.costPerSellableUnit() : null,
                userId);
        recalculateParents(itemId, userId, new HashSet<>());
        return result;
    }

    @Override
    @Transactional
    public void recalculateDependentsForIngredient(UUID masterIngredientId, UUID userId) {
        Set<UUID> visited = new HashSet<>();
        for (ItemDomain parent : itemPersistence.findParentsByIngredient(masterIngredientId)) {
            recalculateTree(parent, userId, visited);
        }
    }

    private void recalculateParents(UUID childItemId, UUID userId, Set<UUID> visited) {
        for (ItemDomain parent : itemPersistence.findParentsByChildItem(childItemId)) {
            recalculateTree(parent, userId, visited);
        }
    }

    private void recalculateTree(ItemDomain item, UUID userId, Set<UUID> visited) {
        if (!visited.add(item.getId())) {
            return;
        }
        RecipeCalculationResult result = calculate(item.getRestaurantId(), item.getId());
        itemPersistence.updateTheoreticalCost(
                item.getId(),
                result.costComplete() ? result.costPerSellableUnit() : null,
                userId);
        recalculateParents(item.getId(), userId, visited);
    }

    private RecipeCalculationResult calculateItem(UUID restaurantId, UUID itemId, Set<UUID> path) {
        if (!path.add(itemId)) {
            throw new IllegalArgumentException("Circular recipe reference detected.");
        }

        ItemDomain item = itemPersistence.findById(itemId)
                .filter(found -> restaurantId.equals(found.getRestaurantId()))
                .orElseThrow(() -> ItemNotFoundException.forId(itemId));
        List<RecipeLineDomain> lines = itemPersistence.findRecipeLinesByParent(itemId);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Tracked items require at least one recipe line.");
        }
        if (item.getYieldQty() == null || item.getYieldQty().signum() <= 0 || item.getYieldUnitId() == null) {
            throw new IllegalArgumentException("Recipe yield must be positive and include a unit.");
        }

        UnitConversion yieldConversion = itemPersistence.getUnitConversion(item.getYieldUnitId());
        BigDecimal physicalYield = scaled(item.getYieldQty().multiply(yieldConversion.factorToBase()));
        Map<UUID, MutableRequirement> batchRequirements = new LinkedHashMap<>();

        for (RecipeLineDomain line : lines) {
            validateLine(line);
            if (line.getSource() == RecipeLineSource.INGREDIENT) {
                IngredientCostSource source = itemPersistence.findIngredientCostSource(line.getMasterIngredientId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Ingredient not found: " + line.getMasterIngredientId()));
                BigDecimal quantityBase = scaled(itemPersistence.convertUnit(
                        line.getUnitId(), source.baseUnitId(), line.getQuantity()));
                merge(batchRequirements, source, quantityBase);
                continue;
            }

            ItemDomain child = itemPersistence.findById(line.getChildItemId())
                    .filter(found -> found.getItemClass() == ItemClass.PREPARATION)
                    .filter(found -> restaurantId.equals(found.getRestaurantId()))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Recipe preparations must belong to the same restaurant."));
            RecipeCalculationResult childResult = calculateItem(restaurantId, child.getId(), path);
            BigDecimal requestedBase = scaled(itemPersistence.convertUnit(
                    line.getUnitId(), childResult.physicalBaseUnitId(), line.getQuantity()));
            BigDecimal factor = requestedBase.divide(
                    childResult.physicalYieldInBase(), SCALE, RoundingMode.HALF_UP);
            for (RecipeIngredientRequirement requirement : childResult.batchRequirements()) {
                merge(batchRequirements, requirement, scaled(requirement.quantityBase().multiply(factor)));
            }
        }
        path.remove(itemId);

        List<RecipeIngredientRequirement> batch = immutableRequirements(batchRequirements);
        List<String> missingCosts = batch.stream()
                .filter(requirement -> requirement.unitCost() == null)
                .map(RecipeIngredientRequirement::ingredientName)
                .distinct()
                .sorted()
                .toList();
        BigDecimal batchCost = missingCosts.isEmpty()
                ? scaled(batch.stream()
                        .map(requirement -> requirement.quantityBase().multiply(requirement.unitCost()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                : null;
        BigDecimal costPerPhysical = batchCost == null
                ? null
                : batchCost.divide(physicalYield, SCALE, RoundingMode.HALF_UP);

        int sellableUnits = item.getItemClass() == ItemClass.PRODUCT
                ? requireSellableUnits(item)
                : 1;
        BigDecimal divisor = BigDecimal.valueOf(sellableUnits);
        List<RecipeIngredientRequirement> perSellableUnit = batch.stream()
                .map(requirement -> new RecipeIngredientRequirement(
                        requirement.masterIngredientId(),
                        requirement.ingredientName(),
                        requirement.baseUnitId(),
                        requirement.baseUnitAbbreviation(),
                        requirement.quantityBase().divide(divisor, SCALE, RoundingMode.HALF_UP),
                        requirement.unitCost()))
                .toList();
        BigDecimal costPerSellable = item.getItemClass() == ItemClass.PRODUCT
                ? (batchCost == null ? null : batchCost.divide(divisor, SCALE, RoundingMode.HALF_UP))
                : costPerPhysical;

        return new RecipeCalculationResult(
                batchCost,
                physicalYield,
                yieldConversion.baseUnitId(),
                yieldConversion.baseUnitAbbreviation(),
                costPerPhysical,
                costPerSellable,
                batch,
                perSellableUnit,
                missingCosts);
    }

    private int requireSellableUnits(ItemDomain item) {
        Integer value = item.getSellableUnitsPerBatch();
        if (value == null || value < 1) {
            throw new IllegalArgumentException("Tracked products require at least one sellable unit per batch.");
        }
        return value;
    }

    private void validateLine(RecipeLineDomain line) {
        if (line.getQuantity() == null || line.getQuantity().signum() <= 0 || line.getUnitId() == null) {
            throw new IllegalArgumentException("Recipe quantities and units are required.");
        }
    }

    private void merge(
            Map<UUID, MutableRequirement> target,
            IngredientCostSource source,
            BigDecimal quantity) {
        target.compute(source.ingredientId(), (id, current) -> {
            if (current == null) {
                return new MutableRequirement(
                        source.ingredientId(), source.ingredientName(), source.baseUnitId(),
                        source.baseUnitAbbreviation(), quantity, source.unitCost());
            }
            current.quantity = scaled(current.quantity.add(quantity));
            if (source.unitCost() == null) {
                current.unitCost = null;
            }
            return current;
        });
    }

    private void merge(
            Map<UUID, MutableRequirement> target,
            RecipeIngredientRequirement source,
            BigDecimal quantity) {
        target.compute(source.masterIngredientId(), (id, current) -> {
            if (current == null) {
                return new MutableRequirement(
                        source.masterIngredientId(), source.ingredientName(), source.baseUnitId(),
                        source.baseUnitAbbreviation(), quantity, source.unitCost());
            }
            current.quantity = scaled(current.quantity.add(quantity));
            if (source.unitCost() == null) {
                current.unitCost = null;
            }
            return current;
        });
    }

    private List<RecipeIngredientRequirement> immutableRequirements(Map<UUID, MutableRequirement> requirements) {
        List<RecipeIngredientRequirement> result = new ArrayList<>();
        requirements.values().stream()
                .sorted(Comparator.comparing(requirement -> requirement.name))
                .forEach(requirement -> result.add(new RecipeIngredientRequirement(
                        requirement.id, requirement.name, requirement.baseUnitId,
                        requirement.baseUnitAbbreviation, scaled(requirement.quantity), requirement.unitCost)));
        return List.copyOf(result);
    }

    private BigDecimal scaled(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static final class MutableRequirement {
        private final UUID id;
        private final String name;
        private final UUID baseUnitId;
        private final String baseUnitAbbreviation;
        private BigDecimal quantity;
        private BigDecimal unitCost;

        private MutableRequirement(
                UUID id,
                String name,
                UUID baseUnitId,
                String baseUnitAbbreviation,
                BigDecimal quantity,
                BigDecimal unitCost) {
            this.id = id;
            this.name = name;
            this.baseUnitId = baseUnitId;
            this.baseUnitAbbreviation = baseUnitAbbreviation;
            this.quantity = quantity;
            this.unitCost = unitCost;
        }
    }
}
