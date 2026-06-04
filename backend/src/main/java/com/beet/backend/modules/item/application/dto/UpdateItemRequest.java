package com.beet.backend.modules.item.application.dto;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateItemRequest(
        String name,
        String description,
        BigDecimal salePrice,
        BigDecimal yieldQty,
        UUID yieldUnitId,
        // For flat products only — user-defined cost
        BigDecimal userDefinedCost,
        Boolean isAvailableAsTemplateOption,
        @Valid List<RecipeLineRequest> lines) {
}
