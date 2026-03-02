package com.beet.backend.modules.inventory.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class InventoryStockDomain {
    private UUID id;
    private UUID masterIngredientId;
    private UUID restaurantId;
    private BigDecimal currentStock;
    private BigDecimal minStock;
}
