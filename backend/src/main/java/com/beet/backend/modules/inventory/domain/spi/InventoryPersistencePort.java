package com.beet.backend.modules.inventory.domain.spi;

import com.beet.backend.modules.inventory.domain.model.InventoryStockDomain;
import com.beet.backend.modules.inventory.domain.model.InventoryTransactionDomain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for inventory persistence operations.
 *
 * Both stock and transaction operations live in the same port because they
 * belong
 * to the same bounded context (Inventory). Transactions are always created in
 * the
 * context of a stock operation, making a separate TransactionPersistencePort
 * unnecessary overhead. This is consistent with IngredientPersistencePort which
 * handles both master_ingredients and supplier_items.
 *
 * If transactions later need independent queries (e.g. cross-restaurant
 * reports),
 * this port can be split at that point without affecting the domain layer.
 */
public interface InventoryPersistencePort {

    /**
     * Saves a new stock entry. Audit fields (@CreatedBy, @CreatedDate) are handled
     * by Spring Data.
     */
    InventoryStockDomain saveStock(InventoryStockDomain stock);

    /**
     * Saves an immutable transaction log entry via raw JDBC (no update audit
     * needed).
     */
    InventoryTransactionDomain saveTransaction(InventoryTransactionDomain transaction);

    Optional<InventoryStockDomain> findStockById(UUID stockId);

    Optional<InventoryStockDomain> findStockByIngredientAndRestaurant(UUID masterIngredientId, UUID restaurantId);

    boolean existsByIngredientAndRestaurant(UUID masterIngredientId, UUID restaurantId);

    List<InventoryStockDomain> findAllStocksByRestaurant(UUID restaurantId);

    /**
     * Updates only the current_stock field. Uses raw JDBC for a targeted
     * single-column
     * update instead of loading the full aggregate, which is more efficient.
     * This is consistent with IngredientJdbcAdapter.updateActiveSupplierItem().
     */
    void updateCurrentStock(UUID stockId, BigDecimal newStock, UUID updatedBy);

    List<InventoryTransactionDomain> findTransactionsByStockId(UUID stockId);
}
