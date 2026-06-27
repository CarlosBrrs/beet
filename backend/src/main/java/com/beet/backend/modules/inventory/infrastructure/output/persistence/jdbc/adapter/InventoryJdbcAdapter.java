package com.beet.backend.modules.inventory.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.inventory.application.dto.InventoryStockResponse;
import com.beet.backend.modules.inventory.application.dto.InventoryTransactionResponse;
import com.beet.backend.modules.inventory.domain.model.InventoryStockDomain;
import com.beet.backend.modules.inventory.domain.model.InventoryTransactionDomain;
import com.beet.backend.modules.inventory.domain.model.TransactionReason;
import com.beet.backend.modules.inventory.domain.spi.InventoryPersistencePort;
import com.beet.backend.modules.inventory.infrastructure.output.persistence.jdbc.mapper.InventoryAggregateMapper;
import com.beet.backend.modules.inventory.infrastructure.output.persistence.jdbc.repository.IngredientStockJdbcRepository;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InventoryJdbcAdapter implements InventoryPersistencePort {

    // ── Write repository (Spring Data JDBC — auto @CreatedDate/@CreatedBy) ──
    private final IngredientStockJdbcRepository stockRepository;
    private final InventoryAggregateMapper mapper;

    // ── Raw JDBC for reads (CQRS) and targeted updates ──
    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcClient jdbcClient;

    // ── Allowed sort columns (prevent SQL injection on dynamic ORDER BY) ──
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "ingredientName", "LOWER(mi.name)",
            "currentStock", "s.current_stock",
            "minStock", "s.min_stock",
            "unitAbbreviation", "u.abbreviation");

    // ═════════════════════════════════════════════════════════════════════════
    // Write Operations
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public InventoryStockDomain saveStock(InventoryStockDomain stock) {
        // ID is null on create → Spring Data JDBC does INSERT → DB generates UUID
        // @CreatedDate, @CreatedBy, @LastModifiedDate, @LastModifiedBy are auto-set
        var saved = stockRepository.save(mapper.toAggregate(stock));
        return mapper.toDomain(saved);
    }

    /**
     * Transactions are immutable (append-only) — they only have
     * created_at/created_by,
     * no updated_at/updated_by. Using raw JDBC here because:
     * 1. They don't fit BaseAuditableAggregate (which has update audit fields)
     * 2. They are never updated after creation
     * 3. The created_by comes from the use case, not from SecurityContext audit
     */
    @Override
    public InventoryTransactionDomain saveTransaction(InventoryTransactionDomain tx) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO inventory_transactions (id, ingredient_stock_id, delta,
                    reason, invoice_id, previous_stock, resulting_stock, notes, created_by)
                VALUES (:id, :stockId, :delta,
                    :reason::transaction_reason, :invoiceId, :previousStock, :resultingStock, :notes, :createdBy)
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("stockId", tx.getIngredientStockId())
                        .addValue("delta", tx.getDelta())
                        .addValue("reason", tx.getReason().name())
                        .addValue("invoiceId", tx.getInvoiceId())
                        .addValue("previousStock", tx.getPreviousStock())
                        .addValue("resultingStock", tx.getResultingStock())
                        .addValue("notes", tx.getNotes())
                        .addValue("createdBy", tx.getCreatedBy()));

        tx.setId(id);
        return tx;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Read Operations (raw JDBC — CQRS pattern)
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public Optional<InventoryStockDomain> findStockById(UUID stockId) {
        List<InventoryStockDomain> results = jdbc.query("""
                SELECT id, master_ingredient_id, restaurant_id, current_stock, min_stock
                FROM ingredient_stocks
                WHERE id = :stockId AND deleted_at IS NULL
                """,
                new MapSqlParameterSource("stockId", stockId),
                (rs, rowNum) -> mapStock(rs));
        return results.stream().findFirst();
    }

    @Override
    public Optional<InventoryStockDomain> findStockByIngredientAndRestaurant(UUID masterIngredientId,
            UUID restaurantId) {
        List<InventoryStockDomain> results = jdbc.query("""
                SELECT id, master_ingredient_id, restaurant_id, current_stock, min_stock
                FROM ingredient_stocks
                WHERE master_ingredient_id = :masterIngredientId
                  AND restaurant_id = :restaurantId
                  AND deleted_at IS NULL
                """,
                new MapSqlParameterSource()
                        .addValue("masterIngredientId", masterIngredientId)
                        .addValue("restaurantId", restaurantId),
                (rs, rowNum) -> mapStock(rs));
        return results.stream().findFirst();
    }

    @Override
    public boolean existsByIngredientAndRestaurant(UUID masterIngredientId, UUID restaurantId) {
        return stockRepository.existsByMasterIngredientIdAndRestaurantId(masterIngredientId, restaurantId);
    }

    @Override
    public List<InventoryStockDomain> findAllStocksByRestaurant(UUID restaurantId) {
        return jdbc.query("""
                SELECT id, master_ingredient_id, restaurant_id, current_stock, min_stock
                FROM ingredient_stocks
                WHERE restaurant_id = :restaurantId AND deleted_at IS NULL
                ORDER BY created_at DESC
                """,
                new MapSqlParameterSource("restaurantId", restaurantId),
                (rs, rowNum) -> mapStock(rs));
    }

    /**
     * Targeted single-column update via raw JDBC — more efficient than loading
     * the full aggregate just to change one field. Follows the same pattern as
     * IngredientJdbcAdapter.updateActiveSupplierItem().
     */
    @Override
    public void updateCurrentStock(UUID stockId, BigDecimal newStock, UUID updatedBy) {
        jdbc.update("""
                UPDATE ingredient_stocks
                SET current_stock = :newStock, updated_at = NOW(), updated_by = :updatedBy
                WHERE id = :stockId
                """,
                new MapSqlParameterSource()
                        .addValue("newStock", newStock)
                        .addValue("updatedBy", updatedBy)
                        .addValue("stockId", stockId));
    }

    @Override
    public List<InventoryTransactionDomain> findTransactionsByStockId(UUID stockId) {
        return jdbc.query("""
                SELECT id, ingredient_stock_id, delta, reason, invoice_id,
                       previous_stock, resulting_stock, notes, created_at, created_by
                FROM inventory_transactions
                WHERE ingredient_stock_id = :stockId
                ORDER BY created_at DESC
                """,
                new MapSqlParameterSource("stockId", stockId),
                (rs, rowNum) -> mapTransaction(rs));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Enriched Read Operations (used by InventoryHandlerImpl)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Finds a single stock with joined ingredient name and unit abbreviation.
     */
    public InventoryStockResponse findStockResponseById(UUID stockId) {
        return jdbc.queryForObject("""
                SELECT s.id, s.master_ingredient_id, mi.name AS ingredient_name,
                       u.abbreviation AS unit_abbreviation,
                       s.current_stock, s.min_stock
                FROM ingredient_stocks s
                JOIN master_ingredients mi ON mi.id = s.master_ingredient_id
                JOIN units u ON u.id = mi.base_unit_id
                WHERE s.id = :stockId AND s.deleted_at IS NULL
                """,
                new MapSqlParameterSource("stockId", stockId),
                (rs, rowNum) -> mapStockResponse(rs));
    }

    /**
     * Lists all activated stocks for a restaurant, enriched with ingredient info.
     */
    public List<InventoryStockResponse> findAllStockResponses(UUID restaurantId) {
        return jdbc.query("""
                SELECT s.id, s.master_ingredient_id, mi.name AS ingredient_name,
                       u.abbreviation AS unit_abbreviation,
                       s.current_stock, s.min_stock
                FROM ingredient_stocks s
                JOIN master_ingredients mi ON mi.id = s.master_ingredient_id
                JOIN units u ON u.id = mi.base_unit_id
                WHERE s.restaurant_id = :restaurantId AND s.deleted_at IS NULL
                ORDER BY mi.name ASC
                """,
                new MapSqlParameterSource("restaurantId", restaurantId),
                (rs, rowNum) -> mapStockResponse(rs));
    }

    /**
     * Lists master ingredients that belong to the owner but are NOT yet activated
     * in this restaurant.
     * Returns them as InventoryStockResponse with currentStock=0/minStock=0 for UI
     * consistency.
     */
    public List<InventoryStockResponse> findAvailableIngredients(UUID restaurantId, UUID ownerId) {
        return jdbc.query("""
                SELECT mi.id AS id, mi.id AS master_ingredient_id, mi.name AS ingredient_name,
                       u.abbreviation AS unit_abbreviation,
                       0 AS current_stock, 0 AS min_stock
                FROM master_ingredients mi
                JOIN units u ON u.id = mi.base_unit_id
                WHERE mi.owner_id = :ownerId
                  AND mi.deleted_at IS NULL
                  AND mi.id NOT IN (
                      SELECT master_ingredient_id FROM ingredient_stocks
                      WHERE restaurant_id = :restaurantId AND deleted_at IS NULL
                  )
                ORDER BY mi.name ASC
                """,
                new MapSqlParameterSource()
                        .addValue("restaurantId", restaurantId)
                        .addValue("ownerId", ownerId),
                (rs, rowNum) -> mapStockResponse(rs));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Paginated Enriched Read Operations (used by InventoryHandlerImpl)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Paginated list of activated stocks for a restaurant, enriched with ingredient
     * info.
     * Supports search by ingredient name and dynamic sorting.
     */
    public PageResponse<InventoryStockResponse> findAllStockResponsesPaged(
            UUID restaurantId, int page, int size,
            String search, String sortBy, boolean sortDesc) {

        String orderColumn = SORT_COLUMNS.getOrDefault(sortBy, "LOWER(mi.name)");
        String orderDir = sortDesc ? "DESC" : "ASC";
        String orderClause = orderColumn + " " + orderDir;

        boolean hasSearch = search != null && !search.isBlank();

        StringBuilder baseWhere = new StringBuilder("""
                FROM ingredient_stocks s
                JOIN master_ingredients mi ON mi.id = s.master_ingredient_id
                JOIN units u ON u.id = mi.base_unit_id
                WHERE s.restaurant_id = :restaurantId AND s.deleted_at IS NULL
                """);

        if (hasSearch) {
            baseWhere.append("  AND mi.name ILIKE '%' || :search || '%'\n");
        }

        String selectSql = "SELECT s.id, s.master_ingredient_id, mi.name AS ingredient_name, "
                + "u.abbreviation AS unit_abbreviation, s.current_stock, s.min_stock "
                + baseWhere.toString()
                + " ORDER BY " + orderClause
                + " LIMIT :size OFFSET :offset";

        String countSql = "SELECT COUNT(*) " + baseWhere.toString();

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("restaurantId", restaurantId);
        paramMap.put("size", size);
        paramMap.put("offset", (long) page * size);
        if (hasSearch) {
            paramMap.put("search", search);
        }

        Long totalElements = jdbcClient.sql(countSql).params(paramMap)
                .query(Long.class).single();
        if (totalElements == null)
            totalElements = 0L;

        List<InventoryStockResponse> content = jdbcClient.sql(selectSql).params(paramMap)
                .query((rs, rowNum) -> mapStockResponse(rs)).list();

        return PageResponse.of(content, totalElements, page, size);
    }

    /**
     * Paginated list of available (not yet activated) ingredients from the owner's
     * catalog.
     * Supports search by ingredient name.
     */
    public PageResponse<InventoryStockResponse> findAvailableIngredientsPaged(
            UUID restaurantId, UUID ownerId, int page, int size, String search) {

        boolean hasSearch = search != null && !search.isBlank();

        StringBuilder baseWhere = new StringBuilder("""
                FROM master_ingredients mi
                JOIN units u ON u.id = mi.base_unit_id
                WHERE mi.owner_id = :ownerId
                  AND mi.deleted_at IS NULL
                  AND mi.id NOT IN (
                      SELECT master_ingredient_id FROM ingredient_stocks
                      WHERE restaurant_id = :restaurantId AND deleted_at IS NULL
                  )
                """);

        if (hasSearch) {
            baseWhere.append("  AND mi.name ILIKE '%' || :search || '%'\n");
        }

        String selectSql = "SELECT mi.id AS id, mi.id AS master_ingredient_id, mi.name AS ingredient_name, "
                + "u.abbreviation AS unit_abbreviation, 0 AS current_stock, 0 AS min_stock "
                + baseWhere.toString()
                + " ORDER BY LOWER(mi.name) ASC"
                + " LIMIT :size OFFSET :offset";

        String countSql = "SELECT COUNT(*) " + baseWhere.toString();

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("restaurantId", restaurantId);
        paramMap.put("ownerId", ownerId);
        paramMap.put("size", size);
        paramMap.put("offset", (long) page * size);
        if (hasSearch) {
            paramMap.put("search", search);
        }

        Long totalElements = jdbcClient.sql(countSql).params(paramMap)
                .query(Long.class).single();
        if (totalElements == null)
            totalElements = 0L;

        List<InventoryStockResponse> content = jdbcClient.sql(selectSql).params(paramMap)
                .query((rs, rowNum) -> mapStockResponse(rs)).list();

        return PageResponse.of(content, totalElements, page, size);
    }

    /**
     * Paginated transaction history for a specific stock entry.
     */
    public PageResponse<InventoryTransactionResponse> findTransactionsByStockIdPaged(
            UUID stockId, int page, int size) {

        String baseSql = "FROM inventory_transactions WHERE ingredient_stock_id = :stockId";

        Long totalElements = jdbcClient.sql("SELECT COUNT(*) " + baseSql)
                .param("stockId", stockId)
                .query(Long.class).single();
        if (totalElements == null)
            totalElements = 0L;

        List<InventoryTransactionResponse> content = jdbcClient.sql(
                "SELECT id, ingredient_stock_id, delta, reason, invoice_id, "
                        + "previous_stock, resulting_stock, notes, created_at, created_by "
                        + baseSql
                        + " ORDER BY created_at DESC LIMIT :size OFFSET :offset")
                .param("stockId", stockId)
                .param("size", size)
                .param("offset", (long) page * size)
                .query((rs, rowNum) -> mapTransactionResponse(rs)).list();

        return PageResponse.of(content, totalElements, page, size);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Row Mappers
    // ═════════════════════════════════════════════════════════════════════════

    private InventoryStockDomain mapStock(ResultSet rs) throws SQLException {
        return InventoryStockDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .masterIngredientId(rs.getObject("master_ingredient_id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .currentStock(rs.getBigDecimal("current_stock"))
                .minStock(rs.getBigDecimal("min_stock"))
                .build();
    }

    private InventoryTransactionDomain mapTransaction(ResultSet rs) throws SQLException {
        return InventoryTransactionDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .ingredientStockId(rs.getObject("ingredient_stock_id", UUID.class))
                .delta(rs.getBigDecimal("delta"))
                .reason(TransactionReason.valueOf(rs.getString("reason")))
                .invoiceId(rs.getObject("invoice_id", UUID.class))
                .previousStock(rs.getBigDecimal("previous_stock"))
                .resultingStock(rs.getBigDecimal("resulting_stock"))
                .notes(rs.getString("notes"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .build();
    }

    private InventoryStockResponse mapStockResponse(ResultSet rs) throws SQLException {
        BigDecimal currentStock = rs.getBigDecimal("current_stock");
        BigDecimal minStock = rs.getBigDecimal("min_stock");
        boolean lowStock = currentStock.compareTo(minStock) <= 0 && minStock.compareTo(BigDecimal.ZERO) > 0;

        return new InventoryStockResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("master_ingredient_id", UUID.class),
                rs.getString("ingredient_name"),
                rs.getString("unit_abbreviation"),
                currentStock,
                minStock,
                lowStock);
    }

    private InventoryTransactionResponse mapTransactionResponse(ResultSet rs) throws SQLException {
        return new InventoryTransactionResponse(
                rs.getObject("id", UUID.class),
                rs.getBigDecimal("delta"),
                TransactionReason.valueOf(rs.getString("reason")),
                rs.getObject("invoice_id", UUID.class),
                rs.getBigDecimal("previous_stock"),
                rs.getBigDecimal("resulting_stock"),
                rs.getString("notes"),
                rs.getObject("created_at", OffsetDateTime.class));
    }
}
