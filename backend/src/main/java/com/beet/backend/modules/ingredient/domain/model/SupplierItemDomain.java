package com.beet.backend.modules.ingredient.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
public class SupplierItemDomain {
    private UUID id;
    private UUID masterIngredientId;
    private UUID supplierId;
    private String brandName;
    private String purchaseUnitName;
    private BigDecimal conversionFactor; // Final computed factor (e.g. 25 × 1000 = 25000)
    private BigDecimal lastCostBase; // Cost per base unit (e.g. 45000 / 25000 = 1.8)
}
