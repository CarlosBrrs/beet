package com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.ingredient.application.dto.IngredientDetailResponse;
import com.beet.backend.modules.ingredient.application.dto.IngredientListResponse;
import com.beet.backend.modules.ingredient.application.port.out.IngredientQueryPort;
import com.beet.backend.modules.ingredient.domain.model.MasterIngredientDomain;
import com.beet.backend.modules.ingredient.domain.model.SupplierItemDomain;
import com.beet.backend.modules.ingredient.domain.spi.IngredientPersistencePort;
import com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.mapper.IngredientAggregateMapper;
import com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.repository.MasterIngredientJdbcRepository;
import com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.repository.SupplierItemJdbcRepository;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IngredientJdbcAdapter implements IngredientPersistencePort, IngredientQueryPort {

        private final MasterIngredientJdbcRepository ingredientRepository;
        private final SupplierItemJdbcRepository supplierItemRepository;
        private final IngredientAggregateMapper mapper;
        private final JdbcTemplate jdbcTemplate;
        private final JdbcClient jdbcClient;

        private static final Map<String, String> SORT_COLUMNS = Map.of(
                        "name", "LOWER(mi.name)",
                        "costPerBaseUnit", "si.last_cost_base",
                        "unitAbbreviation", "u.abbreviation",
                        "currentStock", "current_stock");

        @Override
        public MasterIngredientDomain saveMasterIngredient(MasterIngredientDomain ingredient) {
                if (ingredient.getId() == null) {
                        var saved = ingredientRepository.save(mapper.toAggregate(ingredient));
                        return mapper.toDomain(saved);
                }
                return jdbcClient.sql("""
                                UPDATE master_ingredients
                                   SET name = :name,
                                       base_unit_id = :baseUnitId,
                                       active_supplier_item_id = :activeSupplierItemId,
                                       updated_at = NOW()
                                 WHERE id = :id
                                   AND owner_id = :ownerId
                                   AND deleted_at IS NULL
                             RETURNING id, owner_id, name, base_unit_id, active_supplier_item_id
                                """)
                                .param("id", ingredient.getId())
                                .param("ownerId", ingredient.getOwnerId())
                                .param("name", ingredient.getName())
                                .param("baseUnitId", ingredient.getBaseUnitId())
                                .param("activeSupplierItemId", ingredient.getActiveSupplierItemId())
                                .query((rs, rowNum) -> MasterIngredientDomain.builder()
                                                .id(rs.getObject("id", UUID.class))
                                                .ownerId(rs.getObject("owner_id", UUID.class))
                                                .name(rs.getString("name"))
                                                .baseUnitId(rs.getObject("base_unit_id", UUID.class))
                                                .activeSupplierItemId(rs.getObject("active_supplier_item_id", UUID.class))
                                                .build())
                                .optional()
                                .orElseThrow(() -> new IllegalArgumentException("Ingredient not found."));
        }

        @Override
        public SupplierItemDomain saveSupplierItem(SupplierItemDomain supplierItem) {
                var saved = supplierItemRepository.save(mapper.toAggregate(supplierItem));
                return mapper.toDomain(saved);
        }

        @Override
        public void updateActiveSupplierItem(UUID masterIngredientId, UUID supplierItemId) {
                jdbcTemplate.update(
                                "UPDATE master_ingredients SET active_supplier_item_id = ?, updated_at = NOW() WHERE id = ?",
                                supplierItemId, masterIngredientId);
        }

        @Override
        public boolean existsByNameAndOwnerId(String name, UUID ownerId) {
                Boolean exists = jdbcClient.sql("""
                                SELECT COUNT(1) > 0
                                FROM master_ingredients
                                WHERE owner_id = :ownerId
                                  AND LOWER(name) = LOWER(:name)
                                  AND deleted_at IS NULL
                                """)
                                .param("ownerId", ownerId)
                                .param("name", name)
                                .query(Boolean.class)
                                .single();
                return Boolean.TRUE.equals(exists);
        }

        @Override
        public boolean existsByNameAndOwnerIdExcludingId(String name, UUID ownerId, UUID excludedId) {
                Boolean exists = jdbcClient.sql("""
                                SELECT COUNT(1) > 0
                                FROM master_ingredients
                                WHERE owner_id = :ownerId
                                  AND LOWER(name) = LOWER(:name)
                                  AND id <> :excludedId
                                  AND deleted_at IS NULL
                                """)
                                .param("ownerId", ownerId)
                                .param("name", name)
                                .param("excludedId", excludedId)
                                .query(Boolean.class)
                                .single();
                return Boolean.TRUE.equals(exists);
        }

        @Override
        public boolean existsByIdAndOwnerId(UUID ingredientId, UUID ownerId) {
                Boolean exists = jdbcClient.sql("""
                                SELECT COUNT(1) > 0
                                FROM master_ingredients
                                WHERE id = :ingredientId
                                  AND owner_id = :ownerId
                                  AND deleted_at IS NULL
                                """)
                                .param("ingredientId", ingredientId)
                                .param("ownerId", ownerId)
                                .query(Boolean.class)
                                .single();
                return Boolean.TRUE.equals(exists);
        }

        @Override
        public Optional<MasterIngredientDomain> findByIdAndOwnerId(UUID ingredientId, UUID ownerId) {
                return jdbcClient.sql("""
                                SELECT id, owner_id, name, base_unit_id, active_supplier_item_id
                                FROM master_ingredients
                                WHERE id = :ingredientId
                                  AND owner_id = :ownerId
                                  AND deleted_at IS NULL
                                """)
                                .param("ingredientId", ingredientId)
                                .param("ownerId", ownerId)
                                .query((rs, rowNum) -> MasterIngredientDomain.builder()
                                                .id(rs.getObject("id", UUID.class))
                                                .ownerId(rs.getObject("owner_id", UUID.class))
                                                .name(rs.getString("name"))
                                                .baseUnitId(rs.getObject("base_unit_id", UUID.class))
                                                .activeSupplierItemId(rs.getObject("active_supplier_item_id", UUID.class))
                                                .build())
                                .optional();
        }

        @Override
        public boolean hasBaseUnitChangeBlockers(UUID ingredientId) {
                Boolean exists = jdbcClient.sql("""
                                SELECT EXISTS (
                                    SELECT 1 FROM supplier_items WHERE master_ingredient_id = :ingredientId AND deleted_at IS NULL
                                    UNION ALL
                                    SELECT 1 FROM ingredient_stocks WHERE master_ingredient_id = :ingredientId AND deleted_at IS NULL
                                    UNION ALL
                                    SELECT 1 FROM recipe_lines WHERE master_ingredient_id = :ingredientId
                                    UNION ALL
                                    SELECT 1 FROM order_item_ingredient_requirements WHERE master_ingredient_id = :ingredientId
                                    UNION ALL
                                    SELECT 1 FROM inventory_reservations WHERE master_ingredient_id = :ingredientId
                                    UNION ALL
                                    SELECT 1 FROM order_item_consumptions WHERE master_ingredient_id = :ingredientId
                                )
                                """)
                                .param("ingredientId", ingredientId)
                                .query(Boolean.class)
                                .single();
                return Boolean.TRUE.equals(exists);
        }

        @Override
        public boolean hasDeleteBlockers(UUID ingredientId) {
                Boolean exists = jdbcClient.sql("""
                                SELECT EXISTS (
                                    SELECT 1 FROM ingredient_stocks WHERE master_ingredient_id = :ingredientId AND deleted_at IS NULL
                                    UNION ALL
                                    SELECT 1 FROM recipe_lines WHERE master_ingredient_id = :ingredientId
                                    UNION ALL
                                    SELECT 1 FROM order_item_ingredient_requirements WHERE master_ingredient_id = :ingredientId
                                    UNION ALL
                                    SELECT 1 FROM inventory_reservations WHERE master_ingredient_id = :ingredientId
                                    UNION ALL
                                    SELECT 1 FROM order_item_consumptions WHERE master_ingredient_id = :ingredientId
                                    UNION ALL
                                    SELECT 1 FROM inventory_transactions it JOIN ingredient_stocks ist ON ist.id = it.ingredient_stock_id WHERE ist.master_ingredient_id = :ingredientId
                                )
                                """)
                                .param("ingredientId", ingredientId)
                                .query(Boolean.class)
                                .single();
                return Boolean.TRUE.equals(exists);
        }

        @Override
        public void softDelete(UUID ingredientId, UUID ownerId, UUID actorId) {
                jdbcClient.sql("""
                                UPDATE master_ingredients
                                   SET deleted_at = NOW(),
                                       deleted_by = :actorId,
                                       updated_at = NOW()
                                 WHERE id = :ingredientId
                                   AND owner_id = :ownerId
                                   AND deleted_at IS NULL
                                """)
                                .param("ingredientId", ingredientId)
                                .param("ownerId", ownerId)
                                .param("actorId", actorId)
                                .update();
        }

        @Override
        public PageResponse<IngredientListResponse> findAllByOwnerId(
                        UUID ownerId, int page, int size,
                        String search, String sortBy, boolean sortDesc, List<String> units) {

                int safePage = Math.max(page, 0);
                int safeSize = Math.min(Math.max(size, 1), 100);
                String orderColumn = SORT_COLUMNS.getOrDefault(sortBy, "LOWER(mi.name)");
                String orderDir = sortDesc ? "DESC" : "ASC";
                String orderClause = "costPerBaseUnit".equals(sortBy)
                                ? orderColumn + " " + orderDir + " NULLS LAST"
                                : orderColumn + " " + orderDir;

                boolean hasSearch = search != null && !search.isBlank();
                boolean hasUnits = units != null && !units.isEmpty();

                StringBuilder baseWhere = new StringBuilder("""
                                FROM master_ingredients mi
                                JOIN units u ON mi.base_unit_id = u.id
                                LEFT JOIN supplier_items si ON mi.active_supplier_item_id = si.id AND si.deleted_at IS NULL
                                LEFT JOIN (
                                    SELECT master_ingredient_id, COALESCE(SUM(current_stock), 0) AS current_stock
                                    FROM ingredient_stocks
                                    WHERE deleted_at IS NULL
                                    GROUP BY master_ingredient_id
                                ) stock ON stock.master_ingredient_id = mi.id
                                WHERE mi.owner_id = :ownerId
                                  AND mi.deleted_at IS NULL
                                """);

                if (hasSearch) {
                        baseWhere.append("  AND mi.name ILIKE '%' || :search || '%'\n");
                }

                if (hasUnits) {
                        baseWhere.append("  AND u.abbreviation IN (:units)\n");
                }

                String selectSql = "SELECT mi.id, mi.name, mi.base_unit_id, u.abbreviation AS unit_abbreviation, "
                                + "si.last_cost_base AS cost_per_base_unit "
                                + baseWhere
                                + " ORDER BY " + orderClause
                                + " LIMIT :size OFFSET :offset";

                String countSql = "SELECT COUNT(*) " + baseWhere;

                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("ownerId", ownerId);
                paramMap.put("size", safeSize);
                paramMap.put("offset", (long) safePage * safeSize);

                if (hasSearch) {
                        paramMap.put("search", search.trim());
                }
                if (hasUnits) {
                        paramMap.put("units", units);
                }

                Long totalElements = jdbcClient.sql(countSql)
                                .params(paramMap)
                                .query(Long.class)
                                .single();
                if (totalElements == null) {
                        totalElements = 0L;
                }

                List<IngredientListResponse> content = jdbcClient.sql(selectSql)
                                .params(paramMap)
                                .query((rs, rowNum) -> new IngredientListResponse(
                                                rs.getObject("id", UUID.class),
                                                rs.getString("name"),
                                                rs.getObject("base_unit_id", UUID.class),
                                                rs.getString("unit_abbreviation"),
                                                rs.getBigDecimal("cost_per_base_unit")))
                                .list();

                return PageResponse.of(content, totalElements, safePage, safeSize);
        }

        @Override
        public Optional<IngredientDetailResponse> findDetailById(UUID id, UUID ownerId) {
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
                                       sup.name        AS supplier_name,
                                       COALESCE(stock.current_stock, 0) AS current_stock
                                FROM master_ingredients mi
                                JOIN units u ON mi.base_unit_id = u.id
                                LEFT JOIN supplier_items si  ON mi.active_supplier_item_id = si.id AND si.deleted_at IS NULL
                                LEFT JOIN suppliers     sup  ON si.supplier_id = sup.id AND sup.deleted_at IS NULL
                                LEFT JOIN (
                                    SELECT master_ingredient_id, COALESCE(SUM(current_stock), 0) AS current_stock
                                    FROM ingredient_stocks
                                    WHERE deleted_at IS NULL
                                    GROUP BY master_ingredient_id
                                ) stock ON stock.master_ingredient_id = mi.id
                                WHERE mi.id = :id
                                  AND mi.owner_id = :ownerId
                                  AND mi.deleted_at IS NULL
                                """;

                return jdbcClient.sql(sql)
                                .param("id", id)
                                .param("ownerId", ownerId)
                                .query((rs, rowNum) -> {
                                        String supplierItemIdStr = rs.getString("supplier_item_id");
                                        String supplierIdStr = rs.getString("supplier_id");

                                        IngredientDetailResponse.ActiveSupplierInfo supplierInfo = null;
                                        if (supplierItemIdStr != null && supplierIdStr != null) {
                                                supplierInfo = new IngredientDetailResponse.ActiveSupplierInfo(
                                                                UUID.fromString(supplierIdStr),
                                                                rs.getString("supplier_name"),
                                                                UUID.fromString(supplierItemIdStr),
                                                                rs.getString("brand_name"),
                                                                rs.getString("purchase_unit_name"),
                                                                rs.getBigDecimal("conversion_factor"),
                                                                rs.getBigDecimal("last_cost_base"));
                                        }

                                        BigDecimal cost = rs.getBigDecimal("last_cost_base");
                                        return new IngredientDetailResponse(
                                                        rs.getObject("id", UUID.class),
                                                        rs.getString("name"),
                                                        rs.getObject("base_unit_id", UUID.class),
                                                        rs.getString("unit_name"),
                                                        rs.getString("unit_abbreviation"),
                                                        cost,
                                                        rs.getBigDecimal("current_stock"),
                                                        cost != null,
                                                        supplierInfo);
                                })
                                .optional();
        }
}

