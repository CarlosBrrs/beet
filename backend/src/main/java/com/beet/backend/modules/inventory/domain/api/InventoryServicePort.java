package com.beet.backend.modules.inventory.domain.api;

import com.beet.backend.modules.inventory.domain.model.InventoryStockDomain;
import com.beet.backend.modules.inventory.domain.model.InventoryTransactionDomain;
import com.beet.backend.modules.inventory.domain.model.TransactionReason;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Input port for inventory business operations.
 */
public interface InventoryServicePort {

        /**
         * Activates a master ingredient in a restaurant with an initial stock level.
         * The handler builds the InventoryStockDomain and passes it in.
         */
        InventoryStockDomain activate(InventoryStockDomain stock, UUID ownerId, UUID userId);

        /**
         * Lists all activated ingredient stocks for a restaurant.
         */
        List<InventoryStockDomain> listStocks(UUID restaurantId);

        /**
         * Adjusts stock via REPLACE or DELTA mode.
         */
        InventoryTransactionDomain adjustStock(UUID stockId, BigDecimal value,
                        boolean isReplace, TransactionReason reason,
                        String notes, UUID userId);

        /**
         * Returns the transaction history for a specific stock.
         */
        List<InventoryTransactionDomain> getTransactions(UUID stockId);
}
