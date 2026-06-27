package com.beet.backend.modules.inventory.application.handler;

import com.beet.backend.modules.inventory.application.dto.*;
import com.beet.backend.modules.inventory.application.mapper.InventoryServiceMapper;
import com.beet.backend.modules.inventory.domain.api.InventoryServicePort;
import com.beet.backend.modules.inventory.domain.model.InventoryStockDomain;
import com.beet.backend.modules.inventory.domain.model.InventoryTransactionDomain;
import com.beet.backend.modules.inventory.domain.model.TransactionReason;
import com.beet.backend.modules.inventory.infrastructure.output.persistence.jdbc.adapter.InventoryJdbcAdapter;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InventoryHandlerImpl implements InventoryHandler {

        private final InventoryServicePort inventoryService;
        private final InventoryJdbcAdapter inventoryAdapter; // for enriched queries
        private final InventoryServiceMapper mapper;

        /** Allowed reasons for manual adjustments (block INITIAL, PURCHASE, SALE) */
        private static final Set<TransactionReason> MANUAL_REASONS = Set.of(
                        TransactionReason.ADJUSTMENT, TransactionReason.WASTE, TransactionReason.CORRECTION);

        @Override
        public ApiGenericResponse<InventoryStockResponse> activate(ActivateIngredientRequest request,
                        UUID restaurantId, UUID ownerId, UUID userId) {

                InventoryStockDomain stock = mapper.toStockDomain(request, restaurantId);
                InventoryStockDomain saved = inventoryService.activate(stock, ownerId, userId);

                // Return enriched response (joined with ingredient name + unit)
                InventoryStockResponse response = inventoryAdapter.findStockResponseById(saved.getId());
                return ApiGenericResponse.success(response);
        }

        @Override
        public ApiGenericResponse<PageResponse<InventoryStockResponse>> listStocks(
                        UUID restaurantId, int page, int size,
                        String search, String sortBy, boolean sortDesc) {
                return ApiGenericResponse.success(
                                inventoryAdapter.findAllStockResponsesPaged(restaurantId, page, size, search, sortBy,
                                                sortDesc));
        }

        @Override
        public ApiGenericResponse<List<InventoryStockResponse>> listAvailable(
                        UUID restaurantId, UUID ownerId) {
                List<InventoryStockResponse> available = inventoryAdapter.findAvailableIngredients(restaurantId,
                                ownerId);
                return ApiGenericResponse.success(available);
        }

        @Override
        public ApiGenericResponse<InventoryTransactionResponse> adjustStock(UUID restaurantId, UUID stockId,
                        AdjustStockRequest request, UUID userId) {
                // Validate reason is allowed for manual adjustments
                if (!MANUAL_REASONS.contains(request.reason())) {
                        throw new IllegalArgumentException(
                                        "Invalid reason for manual adjustment. Allowed: " + MANUAL_REASONS);
                }

                boolean isReplace = request.mode() == AdjustStockRequest.AdjustmentMode.REPLACE;

                InventoryTransactionDomain tx = inventoryService.adjustStock(
                                stockId, request.value(), isReplace,
                                request.reason(), request.notes(), userId);

                return ApiGenericResponse.success(mapper.toTransactionResponse(tx));
        }

        @Override
        public ApiGenericResponse<PageResponse<InventoryTransactionResponse>> getTransactions(
                        UUID stockId, int page, int size) {
                return ApiGenericResponse.success(
                                inventoryAdapter.findTransactionsByStockIdPaged(stockId, page, size));
        }
}
