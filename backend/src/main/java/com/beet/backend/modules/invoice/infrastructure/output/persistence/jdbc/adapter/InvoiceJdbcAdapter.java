package com.beet.backend.modules.invoice.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.invoice.application.dto.InvoiceDetailResponse;
import com.beet.backend.modules.invoice.application.dto.SupplierItemForInvoiceResponse;
import com.beet.backend.modules.invoice.domain.model.InvoiceDomain;
import com.beet.backend.modules.invoice.domain.model.InvoiceItemDomain;
import com.beet.backend.modules.invoice.domain.model.InvoiceStatus;
import com.beet.backend.modules.invoice.domain.spi.InvoicePersistencePort;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * JDBC adapter for invoice persistence using JdbcClient.
 * Handles save (header + items), paginated list with supplier name,
 * and detailed queries with enriched item data.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class InvoiceJdbcAdapter implements InvoicePersistencePort {

    private final JdbcClient jdbc;

    // ═════════════════════════════════════════════════════════════════════════
    // Save
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public InvoiceDomain save(InvoiceDomain invoice) {
        UUID invoiceId = UUID.randomUUID();
        invoice.setId(invoiceId);

        // Insert header
        jdbc.sql("""
                INSERT INTO invoices (id, owner_id, restaurant_id, supplier_id,
                    supplier_invoice_number, emission_date, subtotal, total_tax,
                    total_amount, notes, status, created_by)
                VALUES (:id, :ownerId, :restaurantId, :supplierId,
                    :invoiceNumber, :emissionDate, :subtotal, :totalTax,
                    :totalAmount, :notes, :status, :createdBy)
                """)
                .param("id", invoiceId)
                .param("ownerId", invoice.getOwnerId())
                .param("restaurantId", invoice.getRestaurantId())
                .param("supplierId", invoice.getSupplierId())
                .param("invoiceNumber", invoice.getSupplierInvoiceNumber())
                .param("emissionDate", invoice.getEmissionDate())
                .param("subtotal", invoice.getSubtotal())
                .param("totalTax", invoice.getTotalTax())
                .param("totalAmount", invoice.getTotalAmount())
                .param("notes", invoice.getNotes())
                .param("status", invoice.getStatus().name())
                .param("createdBy", invoice.getCreatedBy())
                .update();

        // Insert items
        for (InvoiceItemDomain item : invoice.getItems()) {
            UUID itemId = UUID.randomUUID();
            item.setId(itemId);
            item.setInvoiceId(invoiceId);

            jdbc.sql("""
                    INSERT INTO invoice_items (id, invoice_id, supplier_item_id,
                        quantity_purchased, unit_price_purchased, tax_percentage,
                        subtotal, tax_amount, conversion_factor_used)
                    VALUES (:id, :invoiceId, :supplierItemId,
                        :quantity, :unitPrice, :taxPct,
                        :subtotal, :taxAmount, :conversionFactor)
                    """)
                    .param("id", itemId)
                    .param("invoiceId", invoiceId)
                    .param("supplierItemId", item.getSupplierItemId())
                    .param("quantity", item.getQuantityPurchased())
                    .param("unitPrice", item.getUnitPricePurchased())
                    .param("taxPct", item.getTaxPercentage())
                    .param("subtotal", item.getSubtotal())
                    .param("taxAmount", item.getTaxAmount())
                    .param("conversionFactor", item.getConversionFactorUsed())
                    .update();
        }

        return invoice;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Find by ID with items
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public Optional<InvoiceDomain> findByIdWithItems(UUID invoiceId) {
        // First load the header
        Optional<InvoiceDomain> optInvoice = jdbc.sql("""
                SELECT i.id, i.owner_id, i.restaurant_id, i.supplier_id,
                       i.supplier_invoice_number, i.emission_date, i.received_at,
                       i.subtotal, i.total_tax, i.total_amount, i.notes, i.status,
                       i.created_at, i.created_by,
                       s.name AS supplier_name
                FROM invoices i
                JOIN suppliers s ON i.supplier_id = s.id
                WHERE i.id = :invoiceId
                """)
                .param("invoiceId", invoiceId)
                .query((rs, rowNum) -> mapInvoice(rs))
                .optional();

        if (optInvoice.isEmpty())
            return Optional.empty();

        InvoiceDomain invoice = optInvoice.get();

        // Load items
        List<InvoiceItemDomain> items = jdbc.sql("""
                SELECT ii.id, ii.invoice_id, ii.supplier_item_id,
                       ii.quantity_purchased, ii.unit_price_purchased, ii.tax_percentage,
                       ii.subtotal, ii.tax_amount, ii.conversion_factor_used
                FROM invoice_items ii
                WHERE ii.invoice_id = :invoiceId
                """)
                .param("invoiceId", invoiceId)
                .query((rs, rowNum) -> mapInvoiceItem(rs))
                .list();

        invoice.setItems(items);
        return Optional.of(invoice);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Paginated list
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public PageResponse<InvoiceDomain> findAllPaged(UUID restaurantId, int page, int size, String search) {
        String baseSql = " FROM invoices i JOIN suppliers s ON i.supplier_id = s.id"
                + " WHERE i.restaurant_id = :restaurantId";

        boolean hasSearch = search != null && !search.isBlank();
        if (hasSearch) {
            baseSql += " AND (LOWER(s.name) LIKE :search OR LOWER(i.supplier_invoice_number) LIKE :search)";
        }

        var countQuery = jdbc.sql("SELECT COUNT(*)" + baseSql).param("restaurantId", restaurantId);
        if (hasSearch) {
            countQuery.param("search", "%" + search.toLowerCase() + "%");
        }
        Long totalElements = countQuery.query(Long.class).single();
        if (totalElements == null)
            totalElements = 0L;

        var listQuery = jdbc.sql(
                "SELECT i.id, i.owner_id, i.restaurant_id, i.supplier_id,"
                        + " i.supplier_invoice_number, i.emission_date, i.received_at,"
                        + " i.subtotal, i.total_tax, i.total_amount, i.notes, i.status,"
                        + " i.created_at, i.created_by,"
                        + " s.name AS supplier_name,"
                        + " (SELECT COUNT(*) FROM invoice_items ii WHERE ii.invoice_id = i.id) AS item_count"
                        + baseSql
                        + " ORDER BY i.emission_date DESC, i.created_at DESC"
                        + " LIMIT :size OFFSET :offset")
                .param("restaurantId", restaurantId)
                .param("size", size)
                .param("offset", (long) page * size);

        if (hasSearch) {
            listQuery.param("search", "%" + search.toLowerCase() + "%");
        }

        List<InvoiceDomain> content = listQuery.query((rs, rowNum) -> mapInvoice(rs)).list();

        return PageResponse.of(content, totalElements, page, size);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Supplier items for invoice form (enriched with ingredient name + unit)
    // ═════════════════════════════════════════════════════════════════════════

    public List<SupplierItemForInvoiceResponse> findSupplierItemsForInvoice(UUID supplierId) {
        return jdbc.sql("""
                SELECT si.id, si.brand_name, si.purchase_unit_name, si.conversion_factor,
                       si.last_cost_base, si.master_ingredient_id,
                       mi.name AS ingredient_name,
                       u.abbreviation AS base_unit_abbreviation
                FROM supplier_items si
                JOIN master_ingredients mi ON si.master_ingredient_id = mi.id
                JOIN units u ON mi.base_unit_id = u.id
                WHERE si.supplier_id = :supplierId AND si.deleted_at IS NULL
                ORDER BY mi.name, si.brand_name
                """)
                .param("supplierId", supplierId)
                .query((rs, rowNum) -> new SupplierItemForInvoiceResponse(
                        rs.getObject("id", UUID.class),
                        rs.getString("brand_name"),
                        rs.getString("purchase_unit_name"),
                        rs.getBigDecimal("conversion_factor"),
                        rs.getBigDecimal("last_cost_base"),
                        rs.getObject("master_ingredient_id", UUID.class),
                        rs.getString("ingredient_name"),
                        rs.getString("base_unit_abbreviation")))
                .list();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Invoice detail with enriched items
    // ═════════════════════════════════════════════════════════════════════════

    public Optional<InvoiceDetailResponse> findDetailById(UUID invoiceId) {
        // Header
        Optional<InvoiceDetailResponse> optResult = jdbc.sql("""
                SELECT i.id, i.supplier_invoice_number, i.emission_date, i.received_at,
                       i.subtotal, i.total_tax, i.total_amount, i.notes, i.status,
                       s.name AS supplier_name
                FROM invoices i
                JOIN suppliers s ON i.supplier_id = s.id
                WHERE i.id = :invoiceId
                """)
                .param("invoiceId", invoiceId)
                .query((rs, rowNum) -> {
                    // Load enriched items
                    List<InvoiceDetailResponse.InvoiceItemDetailResponse> items = loadEnrichedItems(invoiceId);

                    return new InvoiceDetailResponse(
                            rs.getObject("id", UUID.class),
                            rs.getString("supplier_name"),
                            rs.getString("supplier_invoice_number"),
                            rs.getObject("emission_date", LocalDate.class),
                            rs.getObject("received_at", OffsetDateTime.class),
                            rs.getBigDecimal("subtotal"),
                            rs.getBigDecimal("total_tax"),
                            rs.getBigDecimal("total_amount"),
                            rs.getString("notes"),
                            rs.getString("status"),
                            items);
                })
                .optional();

        return optResult;
    }

    private List<InvoiceDetailResponse.InvoiceItemDetailResponse> loadEnrichedItems(UUID invoiceId) {
        return jdbc.sql("""
                SELECT ii.id, ii.quantity_purchased, ii.unit_price_purchased,
                       ii.tax_percentage, ii.subtotal, ii.tax_amount, ii.conversion_factor_used,
                       mi.name AS ingredient_name,
                       si.purchase_unit_name,
                       u.abbreviation AS base_unit_abbreviation
                FROM invoice_items ii
                JOIN supplier_items si ON ii.supplier_item_id = si.id
                JOIN master_ingredients mi ON si.master_ingredient_id = mi.id
                JOIN units u ON mi.base_unit_id = u.id
                WHERE ii.invoice_id = :invoiceId
                """)
                .param("invoiceId", invoiceId)
                .query((rs, rowNum) -> {
                    BigDecimal unitPrice = rs.getBigDecimal("unit_price_purchased");
                    BigDecimal convFactor = rs.getBigDecimal("conversion_factor_used");
                    BigDecimal costPerBase = unitPrice.divide(convFactor, 6, RoundingMode.HALF_UP);

                    return new InvoiceDetailResponse.InvoiceItemDetailResponse(
                            rs.getObject("id", UUID.class),
                            rs.getString("ingredient_name"),
                            rs.getString("purchase_unit_name"),
                            convFactor,
                            rs.getString("base_unit_abbreviation"),
                            rs.getBigDecimal("quantity_purchased"),
                            unitPrice,
                            rs.getBigDecimal("tax_percentage"),
                            rs.getBigDecimal("subtotal"),
                            rs.getBigDecimal("tax_amount"),
                            costPerBase);
                })
                .list();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Row Mappers
    // ═════════════════════════════════════════════════════════════════════════

    private InvoiceDomain mapInvoice(ResultSet rs) throws SQLException {
        InvoiceDomain invoice = InvoiceDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .ownerId(rs.getObject("owner_id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .supplierId(rs.getObject("supplier_id", UUID.class))
                .supplierInvoiceNumber(rs.getString("supplier_invoice_number"))
                .emissionDate(rs.getObject("emission_date", LocalDate.class))
                .receivedAt(rs.getObject("received_at", OffsetDateTime.class))
                .subtotal(rs.getBigDecimal("subtotal"))
                .totalTax(rs.getBigDecimal("total_tax"))
                .totalAmount(rs.getBigDecimal("total_amount"))
                .notes(rs.getString("notes"))
                .status(InvoiceStatus.valueOf(rs.getString("status")))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .build();

        try {
            String supplierName = rs.getString("supplier_name");
            if (supplierName != null) {
                invoice.setSupplierName(supplierName);
            }
        } catch (SQLException e) {
            invoice.setSupplierName("Unknown");
        }

        return invoice;
    }

    private InvoiceItemDomain mapInvoiceItem(ResultSet rs) throws SQLException {
        return InvoiceItemDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .invoiceId(rs.getObject("invoice_id", UUID.class))
                .supplierItemId(rs.getObject("supplier_item_id", UUID.class))
                .quantityPurchased(rs.getBigDecimal("quantity_purchased"))
                .unitPricePurchased(rs.getBigDecimal("unit_price_purchased"))
                .taxPercentage(rs.getBigDecimal("tax_percentage"))
                .subtotal(rs.getBigDecimal("subtotal"))
                .taxAmount(rs.getBigDecimal("tax_amount"))
                .conversionFactorUsed(rs.getBigDecimal("conversion_factor_used"))
                .build();
    }
}
