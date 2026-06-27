package com.beet.backend.modules.item.domain.usecase;

import com.beet.backend.modules.item.domain.model.IngredientCostSource;
import com.beet.backend.modules.item.domain.model.ItemClass;
import com.beet.backend.modules.item.domain.model.ItemDomain;
import com.beet.backend.modules.item.domain.model.RecipeCalculationResult;
import com.beet.backend.modules.item.domain.model.RecipeLineDomain;
import com.beet.backend.modules.item.domain.model.RecipeLineSource;
import com.beet.backend.modules.item.domain.model.UnitConversion;
import com.beet.backend.modules.item.domain.spi.ItemPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeCalculationUseCaseTest {

    @Mock
    private ItemPersistencePort persistence;

    private RecipeCalculationUseCase useCase;
    private UUID restaurantId;
    private UUID gramId;
    private UUID productId;
    private UUID preparationId;
    private UUID meatId;
    private UUID tomatoId;
    private UUID onionId;

    @BeforeEach
    void setUp() {
        useCase = new RecipeCalculationUseCase(persistence);
        restaurantId = UUID.randomUUID();
        gramId = UUID.randomUUID();
        productId = UUID.randomUUID();
        preparationId = UUID.randomUUID();
        meatId = UUID.randomUUID();
        tomatoId = UUID.randomUUID();
        onionId = UUID.randomUUID();

        when(persistence.getUnitConversion(gramId))
                .thenReturn(new UnitConversion(gramId, gramId, "g", BigDecimal.ONE));
        when(persistence.convertUnit(gramId, gramId, new BigDecimal("1500")))
                .thenReturn(new BigDecimal("1500"));
        when(persistence.convertUnit(gramId, gramId, new BigDecimal("200")))
                .thenReturn(new BigDecimal("200"));
        when(persistence.convertUnit(gramId, gramId, new BigDecimal("50")))
                .thenReturn(new BigDecimal("50"));
        when(persistence.convertUnit(gramId, gramId, new BigDecimal("100")))
                .thenReturn(new BigDecimal("100"));

        when(persistence.findById(productId)).thenReturn(Optional.of(item(
                productId, ItemClass.PRODUCT, new BigDecimal("1500"), 10)));
        when(persistence.findById(preparationId)).thenReturn(Optional.of(item(
                preparationId, ItemClass.PREPARATION, new BigDecimal("100"), null)));
        when(persistence.findRecipeLinesByParent(productId)).thenReturn(List.of(
                ingredientLine(meatId, "1500"),
                preparationLine(preparationId, "200")));
        when(persistence.findRecipeLinesByParent(preparationId)).thenReturn(List.of(
                ingredientLine(tomatoId, "50"),
                ingredientLine(onionId, "100")));
        when(persistence.findIngredientCostSource(meatId))
                .thenReturn(Optional.of(cost(meatId, "Carne molida", "23")));
        when(persistence.findIngredientCostSource(tomatoId))
                .thenReturn(Optional.of(cost(tomatoId, "Tomate", "8")));
        when(persistence.findIngredientCostSource(onionId))
                .thenReturn(Optional.of(cost(onionId, "Cebolla roja", "9")));
    }

    @Test
    void calculatesCostAndRecursiveRequirementsPerSellablePortion() {
        RecipeCalculationResult result = useCase.calculate(restaurantId, productId);

        assertThat(result.batchCost()).isEqualByComparingTo("37100.000000");
        assertThat(result.costPerSellableUnit()).isEqualByComparingTo("3710.000000");
        assertThat(result.ingredientRequirementsPerSellableUnit())
                .extracting(requirement -> requirement.ingredientName() + ":" + requirement.quantityBase())
                .containsExactly(
                        "Carne molida:150.000000",
                        "Cebolla roja:20.000000",
                        "Tomate:10.000000");
        assertThat(result.missingCostIngredients()).isEmpty();
    }

    @Test
    void leavesCostNullWhenAnIngredientHasNoActiveCost() {
        when(persistence.findIngredientCostSource(tomatoId))
                .thenReturn(Optional.of(new IngredientCostSource(
                        tomatoId, "Tomate", gramId, "g", null)));

        RecipeCalculationResult result = useCase.calculate(restaurantId, productId);

        assertThat(result.batchCost()).isNull();
        assertThat(result.costPerSellableUnit()).isNull();
        assertThat(result.missingCostIngredients()).containsExactly("Tomate");
        assertThat(result.ingredientRequirementsPerSellableUnit()).hasSize(3);
    }

    private ItemDomain item(UUID id, ItemClass itemClass, BigDecimal yield, Integer portions) {
        return ItemDomain.builder()
                .id(id)
                .restaurantId(restaurantId)
                .itemClass(itemClass)
                .isInventoryTracked(true)
                .yieldQty(yield)
                .yieldUnitId(gramId)
                .sellableUnitsPerBatch(portions)
                .build();
    }

    private RecipeLineDomain ingredientLine(UUID ingredientId, String quantity) {
        return RecipeLineDomain.builder()
                .source(RecipeLineSource.INGREDIENT)
                .masterIngredientId(ingredientId)
                .quantity(new BigDecimal(quantity))
                .unitId(gramId)
                .build();
    }

    private RecipeLineDomain preparationLine(UUID childId, String quantity) {
        return RecipeLineDomain.builder()
                .source(RecipeLineSource.PREPARATION)
                .childItemId(childId)
                .quantity(new BigDecimal(quantity))
                .unitId(gramId)
                .build();
    }

    private IngredientCostSource cost(UUID id, String name, String value) {
        return new IngredientCostSource(id, name, gramId, "g", new BigDecimal(value));
    }
}
