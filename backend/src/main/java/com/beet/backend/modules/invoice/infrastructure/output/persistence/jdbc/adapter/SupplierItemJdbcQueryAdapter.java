package com.beet.backend.modules.invoice.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.ingredient.domain.model.SupplierItemDomain;
import com.beet.backend.modules.invoice.domain.spi.SupplierItemQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC adapter for cross-module supplier item queries.
 * Used by the invoice module to read and update supplier item cost data.
 */
@Repository
@RequiredArgsConstructor
public class SupplierItemJdbcQueryAdapter implements SupplierItemQueryPort {

    private final JdbcClient jdbc;

    @Override
    public Optional<SupplierItemDomain> findById(UUID supplierItemId) {
        return jdbc.sql("""
                SELECT id, master_ingredient_id, supplier_id, brand_name,
                       purchase_unit_name, conversion_factor, last_cost_base
                FROM supplier_items
                WHERE id = :id AND deleted_at IS NULL
                """)
                .param("id", supplierItemId)
                .query((rs, rowNum) -> SupplierItemDomain.builder()
                        .id(rs.getObject("id", UUID.class))
                        .masterIngredientId(rs.getObject("master_ingredient_id", UUID.class))
                        .supplierId(rs.getObject("supplier_id", UUID.class))
                        .brandName(rs.getString("brand_name"))
                        .purchaseUnitName(rs.getString("purchase_unit_name"))
                        .conversionFactor(rs.getBigDecimal("conversion_factor"))
                        .lastCostBase(rs.getBigDecimal("last_cost_base"))
                        .build())
                .optional();
    }

    @Override
    public void updateLastCostBase(UUID supplierItemId, BigDecimal newCostBase) {
        jdbc.sql("""
                UPDATE supplier_items SET last_cost_base = :costBase, updated_at = NOW()
                WHERE id = :id
                """)
                .param("costBase", newCostBase)
                .param("id", supplierItemId)
                .update();
    }

    @Override
    public List<SupplierItemDomain> findBySupplierId(UUID supplierId) {
        return jdbc.sql("""
                SELECT id, master_ingredient_id, supplier_id, brand_name,
                       purchase_unit_name, conversion_factor, last_cost_base
                FROM supplier_items
                WHERE supplier_id = :supplierId AND deleted_at IS NULL
                ORDER BY brand_name
                """)
                .param("supplierId", supplierId)
                .query((rs, rowNum) -> SupplierItemDomain.builder()
                        .id(rs.getObject("id", UUID.class))
                        .masterIngredientId(rs.getObject("master_ingredient_id", UUID.class))
                        .supplierId(rs.getObject("supplier_id", UUID.class))
                        .brandName(rs.getString("brand_name"))
                        .purchaseUnitName(rs.getString("purchase_unit_name"))
                        .conversionFactor(rs.getBigDecimal("conversion_factor"))
                        .lastCostBase(rs.getBigDecimal("last_cost_base"))
                        .build())
                .list();
    }
}
