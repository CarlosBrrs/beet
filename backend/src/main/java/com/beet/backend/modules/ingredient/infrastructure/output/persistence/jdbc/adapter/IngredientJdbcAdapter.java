package com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.ingredient.application.dto.IngredientDetailResponse;
import com.beet.backend.modules.ingredient.application.dto.IngredientListResponse;
import com.beet.backend.modules.ingredient.domain.model.MasterIngredientDomain;
import com.beet.backend.modules.ingredient.domain.model.SupplierItemDomain;
import com.beet.backend.modules.ingredient.domain.spi.IngredientPersistencePort;
import com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.mapper.IngredientAggregateMapper;
import com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.repository.MasterIngredientJdbcRepository;
import com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.repository.SupplierItemJdbcRepository;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IngredientJdbcAdapter implements IngredientPersistencePort {

        // ── Write repositories ────────────────────────────────────────────────────
        private final MasterIngredientJdbcRepository ingredientRepository;
        private final SupplierItemJdbcRepository supplierItemRepository;
        private final IngredientAggregateMapper mapper;
        private final JdbcTemplate jdbcTemplate;
        private final JdbcClient jdbcClient;

        // ── Allowed sort columns (prevent SQL injection on dynamic ORDER BY) ───────
        private static final Map<String, String> SORT_COLUMNS = Map.of(
                        "name", "LOWER(mi.name)", // Use LOWER for case-insensitive human sorting
                        "costPerBaseUnit", "si.last_cost_base",
                        "unitAbbreviation", "u.abbreviation",
                        "currentStock", "LOWER(mi.name)" // fallback until ingredient_stocks exists
        );

        // ═════════════════════════════════════════════════════════════════════════
        // Write Operations
        // ═════════════════════════════════════════════════════════════════════════

        @Override
        public MasterIngredientDomain saveMasterIngredient(MasterIngredientDomain ingredient) {
                var saved = ingredientRepository.save(mapper.toAggregate(ingredient));
                return mapper.toDomain(saved);
        }

        @Override
        public SupplierItemDomain saveSupplierItem(SupplierItemDomain supplierItem) {
                var saved = supplierItemRepository.save(mapper.toAggregate(supplierItem));
                return mapper.toDomain(saved);
        }

        @Override
        public void updateActiveSupplierItem(UUID masterIngredientId, UUID supplierItemId) {
                // Direct UPDATE to avoid loading the full aggregate (avoids circular dependency
                // issues with Spring Data JDBC)
                jdbcTemplate.update(
                                "UPDATE master_ingredients SET active_supplier_item_id = ? WHERE id = ?",
                                supplierItemId, masterIngredientId);
        }

        @Override
        public boolean existsByNameAndOwnerId(String name, UUID ownerId) {
                return ingredientRepository.existsByNameAndOwnerId(name, ownerId);
        }

        // ═════════════════════════════════════════════════════════════════════════
        // Read Operations (CQRS — uses NamedParameterJdbcTemplate with raw SQL)
        // ═════════════════════════════════════════════════════════════════════════

        @Override
        public PageResponse<IngredientListResponse> findAllByOwnerId(
                        UUID ownerId, int page, int size,
                        String search, String sortBy, boolean sortDesc, List<String> units) {

                // 1. DYNAMIC SORTING (SECURITY): We map the frontend's sort key to the actual
                // DB column using an allowlist.
                // This completely prevents SQL Injection, because we never concatenate user
                // input directly into the ORDER BY clause.
                String orderColumn = SORT_COLUMNS.getOrDefault(sortBy, "mi.name");
                String orderDir = sortDesc ? "DESC" : "ASC";

                // Handle NULL sort on cost (so ingredients without a supplier/cost go to the
                // bottom of the list instead of the top)
                String orderClause = "cost".equals(sortBy)
                                ? orderColumn + " " + orderDir + " NULLS LAST"
                                : orderColumn + " " + orderDir;

                boolean hasSearch = search != null && !search.isBlank();
                boolean hasUnits = units != null && !units.isEmpty();

                // 2. REUSABLE WHERE CLAUSE: For server-side pagination, we always need TWO
                // queries:
                // a) One to fetch the actual paginated data rows.
                // b) One to count the TOTAL matching rows (so the frontend knows how many pages
                // exist).
                // We use a StringBuilder to dynamically append filters ONLY if they exist.
                // This avoids PostgreSQL driver errors regarding type inference on 'IS NULL'
                // checks and array expansions.
                StringBuilder baseWhere = new StringBuilder("""
                                FROM master_ingredients mi
                                JOIN units u ON mi.base_unit_id = u.id
                                LEFT JOIN supplier_items si ON mi.active_supplier_item_id = si.id
                                WHERE mi.owner_id = :ownerId
                                  AND mi.deleted_at IS NULL
                                """);

                if (hasSearch) {
                        baseWhere.append("  AND mi.name ILIKE '%' || :search || '%'\n");
                }

                if (hasUnits) {
                        // Spring JdbcClient automatically expands IN (:units) into IN (?, ?, ?) when
                        // passing a List
                        baseWhere.append("  AND u.abbreviation IN (:units)\n");
                }

                String selectSql = "SELECT mi.id, mi.name, u.abbreviation AS unit_abbreviation, "
                                + "si.last_cost_base AS cost_per_base_unit "
                                + baseWhere.toString()
                                + " ORDER BY " + orderClause
                                + " LIMIT :size OFFSET :offset";

                String countSql = "SELECT COUNT(*) " + baseWhere.toString();

                // 3. SECURE PARAMETER BINDING
                // We build the parameters map using Map.of or put(), because JdbcClient
                // requires a Map for named arguments, not a MapSqlParameterSource
                // (unless using .paramSource()).
                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("ownerId", ownerId);
                paramMap.put("size", size);
                paramMap.put("offset", (long) page * size);

                if (hasSearch) {
                        paramMap.put("search", search);
                }
                if (hasUnits) {
                        paramMap.put("units", units);
                }

                // 4. JDBC CLIENT - COUNT QUERY
                Long totalElements = jdbcClient.sql(countSql)
                                .params(paramMap)
                                .query(Long.class)
                                .single();
                if (totalElements == null)
                        totalElements = 0L;

                // 5. JDBC CLIENT - FETCH QUERY: We pass the SELECT SQL, inject the parameters,
                // and provide a RowMapper block. For every row returned by the database (rs),
                // we manually construct
                // our flat List DTO. Finally, .list() executes the query and gathers the
                // results into a List.
                List<IngredientListResponse> content = jdbcClient.sql(selectSql)
                                .params(paramMap)
                                .query((rs, rowNum) -> new IngredientListResponse(
                                                UUID.fromString(rs.getString("id")),
                                                rs.getString("name"),
                                                rs.getString("unit_abbreviation"),
                                                rs.getBigDecimal("cost_per_base_unit") // may be null if no supplier
                                ))
                                .list();

                return PageResponse.of(content, totalElements, page, size);
        }

        @Override
        public Optional<IngredientDetailResponse> findDetailById(UUID id, UUID ownerId) {
                // 1. RAW SQL PROJECTION: We select exactly what the frontend needs across 3
                // different tables.
                // We use LEFT JOINs for supplier configuration because an ingredient might
                // exist but not have any supplier assigned yet.
                String sql = """
                                SELECT mi.id, mi.name, mi.base_unit_id,
                                       u.name          AS unit_name,
                                       u.abbreviation  AS unit_abbreviation,
                                       si.last_cost_base,
                                       si.id           AS supplier_item_id,
                                       si.brand_name,
                                       si.purchase_unit_name,
                                       si.conversion_factor,
                                       sup.id          AS supplier_id,
                                       sup.name        AS supplier_name
                                FROM master_ingredients mi
                                JOIN units u ON mi.base_unit_id = u.id
                                LEFT JOIN supplier_items si  ON mi.active_supplier_item_id = si.id
                                LEFT JOIN suppliers     sup  ON si.supplier_id = sup.id
                                WHERE mi.id = :id
                                  AND mi.owner_id = :ownerId
                                  AND mi.deleted_at IS NULL
                                """;

                return jdbcClient.sql(sql)
                                .param("id", id)
                                .param("ownerId", ownerId)
                                .query((rs, rowNum) -> {
                                        // 2. CUSTOM ROW MAPPER: Since we are reading multiple tables into a single
                                        // nested DTO structure,
                                        // we manually extract the columns. `rs` is the JDBC ResultSet representing the
                                        // current row.

                                        String supplierItemIdStr = rs.getString("supplier_item_id");
                                        String supplierIdStr = rs.getString("supplier_id");

                                        IngredientDetailResponse.ActiveSupplierInfo supplierInfo = null;

                                        // 3. NULL HANDLING: Because we used LEFT JOIN, if the ingredient has no active
                                        // supplier,
                                        // supplier_item_id will be NULL in the DB. We detect that to avoid
                                        // NullPointerExceptions,
                                        // and just leave the inner `supplierInfo` object as null in the main response.
                                        if (supplierItemIdStr != null) {
                                                supplierInfo = new IngredientDetailResponse.ActiveSupplierInfo(
                                                                UUID.fromString(supplierIdStr),
                                                                rs.getString("supplier_name"),
                                                                UUID.fromString(supplierItemIdStr),
                                                                rs.getString("brand_name"),
                                                                rs.getString("purchase_unit_name"),
                                                                rs.getBigDecimal("conversion_factor"),
                                                                rs.getBigDecimal("last_cost_base"));
                                        }

                                        return new IngredientDetailResponse(
                                                        UUID.fromString(rs.getString("id")),
                                                        rs.getString("name"),
                                                        UUID.fromString(rs.getString("base_unit_id")),
                                                        rs.getString("unit_name"),
                                                        rs.getString("unit_abbreviation"),
                                                        rs.getBigDecimal("last_cost_base"),
                                                        supplierInfo);
                                })
                                .optional();
        }
}
