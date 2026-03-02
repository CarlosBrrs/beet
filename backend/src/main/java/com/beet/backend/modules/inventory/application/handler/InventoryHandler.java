package com.beet.backend.modules.inventory.application.handler;

import com.beet.backend.modules.inventory.application.dto.*;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.util.List;
import java.util.UUID;

public interface InventoryHandler {

        ApiGenericResponse<InventoryStockResponse> activate(ActivateIngredientRequest request,
                        UUID restaurantId, UUID ownerId, UUID userId);

        ApiGenericResponse<PageResponse<InventoryStockResponse>> listStocks(
                        UUID restaurantId, int page, int size,
                        String search, String sortBy, boolean sortDesc);

        ApiGenericResponse<List<InventoryStockResponse>> listAvailable(UUID restaurantId, UUID ownerId);

        ApiGenericResponse<InventoryTransactionResponse> adjustStock(UUID restaurantId, UUID stockId,
                        AdjustStockRequest request, UUID userId);

        ApiGenericResponse<PageResponse<InventoryTransactionResponse>> getTransactions(
                        UUID stockId, int page, int size);
}
