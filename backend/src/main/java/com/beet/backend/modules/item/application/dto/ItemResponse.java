package com.beet.backend.modules.item.application.dto;

import com.beet.backend.modules.item.domain.model.ItemClass;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ItemResponse(
                UUID id,
                UUID restaurantId,
                ItemClass itemClass,
                String name,
                String description,
                boolean isInventoryTracked,
                BigDecimal yieldQty,
                UUID yieldUnitId,
                Integer sellableUnitsPerBatch,
                BigDecimal portionSize,
                UUID portionUnitId,
                String portionUnitAbbreviation,
                BigDecimal batchTheoreticalCost,
                BigDecimal salePrice,
                BigDecimal theoreticalCost,
                boolean costComplete,
                List<String> missingCostIngredients,
                boolean isActive,
                boolean isAvailableAsTemplateOption,
                boolean isPublished,
                boolean isUsedAsTemplateOption,
                List<RecipeLineResponse> recipeLines,
                OffsetDateTime createdAt,
                OffsetDateTime updatedAt) {
}
