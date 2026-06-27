package com.beet.backend.modules.inventory.application.mapper;

import com.beet.backend.modules.inventory.application.dto.ActivateIngredientRequest;
import com.beet.backend.modules.inventory.application.dto.InventoryTransactionResponse;
import com.beet.backend.modules.inventory.domain.model.InventoryStockDomain;
import com.beet.backend.modules.inventory.domain.model.InventoryTransactionDomain;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class InventoryServiceMapper {

    public InventoryStockDomain toStockDomain(ActivateIngredientRequest request, UUID restaurantId) {
        return InventoryStockDomain.builder()
                .masterIngredientId(request.masterIngredientId())
                .restaurantId(restaurantId)
                .currentStock(request.initialStock())
                .minStock(request.minStock() != null ? request.minStock() : BigDecimal.ZERO)
                .build();
    }

    public InventoryTransactionResponse toTransactionResponse(InventoryTransactionDomain tx) {
        return new InventoryTransactionResponse(
                tx.getId(), tx.getDelta(), tx.getReason(), tx.getInvoiceId(),
                tx.getPreviousStock(), tx.getResultingStock(), tx.getNotes(), tx.getCreatedAt());
    }
}
