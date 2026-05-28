package com.beet.backend.modules.order.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.order.domain.model.KitchenStatus;
import com.beet.backend.modules.order.domain.model.OrderDomain;
import com.beet.backend.modules.order.domain.model.OrderItemDomain;
import com.beet.backend.modules.order.domain.model.OrderItemTaxDomain;
import com.beet.backend.modules.order.domain.model.OrderStatus;
import com.beet.backend.modules.order.domain.model.OrderTaxDefinition;
import com.beet.backend.modules.order.domain.model.OrderTaxDomain;
import com.beet.backend.modules.order.domain.model.PaymentStatus;
import com.beet.backend.modules.order.domain.model.ServiceType;
import com.beet.backend.modules.order.domain.spi.OrderPersistencePort;
import com.beet.backend.modules.order.domain.spi.OrderTaxQueryPort;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderJdbcAdapter implements OrderPersistencePort, OrderTaxQueryPort {

    private final JdbcClient jdbc;

    @Override
    public OrderDomain save(OrderDomain order) {
        UUID orderId = order.getId() != null ? order.getId() : UUID.randomUUID();
        order.setId(orderId);

        String sql = """
                INSERT INTO orders
                    (id, restaurant_id, order_status, kitchen_status, payment_status,
                     service_type, table_id, customer_name, prepayment_required_snapshot,
                     tax_rate_snapshot, subtotal_gross_snapshot, tax_amount_snapshot, total_gross_snapshot,
                     notes, created_by, updated_by)
                VALUES
                    (:id, :restaurantId, :orderStatus::order_status, :kitchenStatus::kitchen_status, :paymentStatus::payment_status,
                     :serviceType::service_type, :tableId, :customerName, :prepaymentRequired,
                     :taxRate, :subtotalGross, :taxAmount, :totalGross,
                     :notes, :createdBy, :updatedBy)
                RETURNING *
                """;

        OrderDomain saved = jdbc.sql(sql)
                .param("id", orderId)
                .param("restaurantId", order.getRestaurantId())
                .param("orderStatus", order.getOrderStatus().name())
                .param("kitchenStatus", order.getKitchenStatus().name())
                .param("paymentStatus", order.getPaymentStatus().name())
                .param("serviceType", order.getServiceType().name())
                .param("tableId", order.getTableId())
                .param("customerName", order.getCustomerName())
                .param("prepaymentRequired", order.isPrepaymentRequiredSnapshot())
                .param("taxRate", order.getTaxRateSnapshot())
                .param("subtotalGross", order.getSubtotalGrossSnapshot())
                .param("taxAmount", order.getTaxAmountSnapshot())
                .param("totalGross", order.getTotalGrossSnapshot())
                .param("notes", order.getNotes())
                .param("createdBy", order.getCreatedBy())
                .param("updatedBy", order.getUpdatedBy())
                .query(this::mapOrder)
                .single();

        saved.setItems(order.getItems());
        saved.setTaxes(order.getTaxes());
        return saved;
    }

    @Override
    public OrderDomain update(OrderDomain order) {
        String sql = """
                UPDATE orders
                   SET order_status = :orderStatus::order_status,
                       kitchen_status = :kitchenStatus::kitchen_status,
                       payment_status = :paymentStatus::payment_status,
                       service_type = :serviceType::service_type,
                       table_id = :tableId,
                       customer_name = :customerName,
                       prepayment_required_snapshot = :prepaymentRequired,
                       tax_rate_snapshot = :taxRate,
                       subtotal_gross_snapshot = :subtotalGross,
                       tax_amount_snapshot = :taxAmount,
                       total_gross_snapshot = :totalGross,
                       notes = :notes,
                       updated_at = NOW(),
                       updated_by = :updatedBy
                 WHERE id = :id
             RETURNING *
                """;

        OrderDomain updated = jdbc.sql(sql)
                .param("id", order.getId())
                .param("orderStatus", order.getOrderStatus().name())
                .param("kitchenStatus", order.getKitchenStatus().name())
                .param("paymentStatus", order.getPaymentStatus().name())
                .param("serviceType", order.getServiceType().name())
                .param("tableId", order.getTableId())
                .param("customerName", order.getCustomerName())
                .param("prepaymentRequired", order.isPrepaymentRequiredSnapshot())
                .param("taxRate", order.getTaxRateSnapshot())
                .param("subtotalGross", order.getSubtotalGrossSnapshot())
                .param("taxAmount", order.getTaxAmountSnapshot())
                .param("totalGross", order.getTotalGrossSnapshot())
                .param("notes", order.getNotes())
                .param("updatedBy", order.getUpdatedBy())
                .query(this::mapOrder)
                .single();

        updated.setItems(order.getItems());
        updated.setTaxes(order.getTaxes());
        return updated;
    }

    @Override
    public OrderItemDomain saveItem(OrderItemDomain item) {
        UUID itemId = item.getId() != null ? item.getId() : UUID.randomUUID();
        item.setId(itemId);

        String sql = """
                INSERT INTO order_items
                    (id, order_id, item_id, submenu_node_id,
                     item_name_snapshot, unit_price_snapshot, theoretical_cost_snapshot,
                     quantity, subtotal_gross_snapshot, created_by, updated_by)
                VALUES
                    (:id, :orderId, :itemId, :submenuNodeId,
                     :itemName, :unitPrice, :theoreticalCost,
                     :quantity, :subtotalGross, :createdBy, :updatedBy)
                RETURNING *
                """;

        return jdbc.sql(sql)
                .param("id", itemId)
                .param("orderId", item.getOrderId())
                .param("itemId", item.getItemId())
                .param("submenuNodeId", item.getSubmenuNodeId())
                .param("itemName", item.getItemNameSnapshot())
                .param("unitPrice", item.getUnitPriceSnapshot())
                .param("theoreticalCost", item.getTheoreticalCostSnapshot())
                .param("quantity", item.getQuantity())
                .param("subtotalGross", item.getSubtotalGrossSnapshot())
                .param("createdBy", item.getCreatedBy())
                .param("updatedBy", item.getUpdatedBy())
                .query(this::mapOrderItem)
                .single();
    }

    @Override
    public void updateItemQuantity(UUID orderItemId, BigDecimal quantity, BigDecimal subtotalGrossSnapshot,
            UUID updatedBy) {
        jdbc.sql("""
                UPDATE order_items
                   SET quantity = :quantity,
                       subtotal_gross_snapshot = :subtotalGross,
                       updated_at = NOW(),
                       updated_by = :updatedBy
                 WHERE id = :id
                """)
                .param("id", orderItemId)
                .param("quantity", quantity)
                .param("subtotalGross", subtotalGrossSnapshot)
                .param("updatedBy", updatedBy)
                .update();
    }

    @Override
    public void deleteItem(UUID orderItemId) {
        jdbc.sql("DELETE FROM order_items WHERE id = :id")
                .param("id", orderItemId)
                .update();
    }

    @Override
    public void replaceOrderTaxes(UUID orderId, List<OrderTaxDomain> taxes) {
        jdbc.sql("DELETE FROM order_taxes WHERE order_id = :orderId")
                .param("orderId", orderId)
                .update();

        if (taxes == null || taxes.isEmpty()) {
            return;
        }

        for (OrderTaxDomain tax : taxes) {
            UUID taxId = tax.getId() != null ? tax.getId() : UUID.randomUUID();
            tax.setId(taxId);

            jdbc.sql("""
                    INSERT INTO order_taxes
                        (id, order_id, tax_id, tax_name_snapshot, tax_rate_snapshot,
                         tax_base_snapshot, tax_amount_snapshot, created_by)
                    VALUES
                        (:id, :orderId, :taxId, :taxName, :taxRate,
                         :taxBase, :taxAmount, :createdBy)
                    """)
                    .param("id", taxId)
                    .param("orderId", orderId)
                    .param("taxId", tax.getTaxId())
                    .param("taxName", tax.getTaxNameSnapshot())
                    .param("taxRate", tax.getTaxRateSnapshot())
                    .param("taxBase", tax.getTaxBaseSnapshot())
                    .param("taxAmount", tax.getTaxAmountSnapshot())
                    .param("createdBy", tax.getCreatedBy())
                    .update();
        }
    }

    @Override
    public void replaceOrderItemTaxes(UUID orderId, List<OrderItemTaxDomain> taxes) {
        jdbc.sql("""
                DELETE FROM order_item_taxes
                 WHERE order_item_id IN (SELECT id FROM order_items WHERE order_id = :orderId)
                """)
                .param("orderId", orderId)
                .update();

        if (taxes == null || taxes.isEmpty()) {
            return;
        }

        for (OrderItemTaxDomain tax : taxes) {
            UUID taxId = tax.getId() != null ? tax.getId() : UUID.randomUUID();
            tax.setId(taxId);

            jdbc.sql("""
                    INSERT INTO order_item_taxes
                        (id, order_item_id, tax_id, tax_name_snapshot, tax_rate_snapshot,
                         tax_base_snapshot, tax_amount_snapshot, created_by)
                    VALUES
                        (:id, :orderItemId, :taxId, :taxName, :taxRate,
                         :taxBase, :taxAmount, :createdBy)
                    """)
                    .param("id", taxId)
                    .param("orderItemId", tax.getOrderItemId())
                    .param("taxId", tax.getTaxId())
                    .param("taxName", tax.getTaxNameSnapshot())
                    .param("taxRate", tax.getTaxRateSnapshot())
                    .param("taxBase", tax.getTaxBaseSnapshot())
                    .param("taxAmount", tax.getTaxAmountSnapshot())
                    .param("createdBy", tax.getCreatedBy())
                    .update();
        }
    }

    @Override
    public Optional<OrderDomain> findByIdWithItems(UUID orderId) {
        Optional<OrderDomain> order = jdbc.sql("SELECT * FROM orders WHERE id = :id")
                .param("id", orderId)
                .query(this::mapOrder)
                .optional();

        if (order.isEmpty()) {
            return Optional.empty();
        }

        OrderDomain domain = order.get();

        List<OrderItemDomain> items = jdbc.sql("""
                SELECT * FROM order_items
                 WHERE order_id = :orderId
                 ORDER BY created_at ASC, id ASC
                """)
                .param("orderId", orderId)
                .query(this::mapOrderItem)
                .list();

        List<OrderTaxDomain> orderTaxes = jdbc.sql("""
                SELECT * FROM order_taxes
                 WHERE order_id = :orderId
                 ORDER BY created_at ASC, id ASC
                """)
                .param("orderId", orderId)
                .query(this::mapOrderTax)
                .list();

        List<OrderItemTaxDomain> itemTaxes = jdbc.sql("""
                SELECT * FROM order_item_taxes
                 WHERE order_item_id IN (SELECT id FROM order_items WHERE order_id = :orderId)
                 ORDER BY created_at ASC, id ASC
                """)
                .param("orderId", orderId)
                .query(this::mapOrderItemTax)
                .list();

        Map<UUID, List<OrderItemTaxDomain>> taxesByItem = itemTaxes.stream()
                .collect(Collectors.groupingBy(OrderItemTaxDomain::getOrderItemId));
        for (OrderItemDomain item : items) {
            item.setTaxes(taxesByItem.getOrDefault(item.getId(), List.of()));
        }

        domain.setItems(items);
        domain.setTaxes(orderTaxes);
        return Optional.of(domain);
    }

    @Override
    public PageResponse<OrderDomain> findAllPaged(UUID restaurantId, int page, int size, String search) {
        String baseSql = " FROM orders o WHERE o.restaurant_id = :restaurantId";
        boolean hasSearch = search != null && !search.isBlank();
        if (hasSearch) {
            baseSql += " AND (LOWER(o.customer_name) LIKE :search OR LOWER(o.notes) LIKE :search)";
        }

        var countQuery = jdbc.sql("SELECT COUNT(*)" + baseSql)
                .param("restaurantId", restaurantId);
        if (hasSearch) {
            countQuery.param("search", "%" + search.toLowerCase() + "%");
        }

        Long totalElements = countQuery.query(Long.class).single();
        if (totalElements == null) {
            totalElements = 0L;
        }

        var listQuery = jdbc.sql("SELECT o.*" + baseSql
                + " ORDER BY o.created_at DESC, o.id DESC"
                + " LIMIT :size OFFSET :offset")
                .param("restaurantId", restaurantId)
                .param("size", size)
                .param("offset", (long) page * size);
        if (hasSearch) {
            listQuery.param("search", "%" + search.toLowerCase() + "%");
        }

        List<OrderDomain> content = listQuery.query(this::mapOrder).list();
        content.forEach(order -> {
            order.setItems(new ArrayList<>());
            order.setTaxes(new ArrayList<>());
        });

        return PageResponse.of(content, totalElements, page, size);
    }

    @Override
    public List<OrderTaxDefinition> findActiveTaxesByRestaurant(UUID restaurantId) {
        return jdbc.sql("""
                SELECT t.id, t.name, t.percentage
                  FROM restaurant_taxes rt
                  JOIN taxes t ON t.id = rt.tax_id
                 WHERE rt.restaurant_id = :restaurantId
                   AND rt.deleted_at IS NULL
                   AND t.deleted_at IS NULL
                 ORDER BY t.name ASC
                """)
                .param("restaurantId", restaurantId)
                .query((rs, rowNum) -> new OrderTaxDefinition(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getBigDecimal("percentage")))
                .list();
    }

    private OrderDomain mapOrder(ResultSet rs, int rowNum) throws SQLException {
        return OrderDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .orderStatus(OrderStatus.valueOf(rs.getString("order_status")))
                .kitchenStatus(KitchenStatus.valueOf(rs.getString("kitchen_status")))
                .paymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")))
                .serviceType(ServiceType.valueOf(rs.getString("service_type")))
                .tableId(rs.getObject("table_id", UUID.class))
                .customerName(rs.getString("customer_name"))
                .prepaymentRequiredSnapshot(rs.getBoolean("prepayment_required_snapshot"))
                .taxRateSnapshot(rs.getBigDecimal("tax_rate_snapshot"))
                .subtotalGrossSnapshot(rs.getBigDecimal("subtotal_gross_snapshot"))
                .taxAmountSnapshot(rs.getBigDecimal("tax_amount_snapshot"))
                .totalGrossSnapshot(rs.getBigDecimal("total_gross_snapshot"))
                .notes(rs.getString("notes"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .items(new ArrayList<>())
                .taxes(new ArrayList<>())
                .build();
    }

    private OrderItemDomain mapOrderItem(ResultSet rs, int rowNum) throws SQLException {
        return OrderItemDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .orderId(rs.getObject("order_id", UUID.class))
                .itemId(rs.getObject("item_id", UUID.class))
                .submenuNodeId(rs.getObject("submenu_node_id", UUID.class))
                .itemNameSnapshot(rs.getString("item_name_snapshot"))
                .unitPriceSnapshot(rs.getBigDecimal("unit_price_snapshot"))
                .theoreticalCostSnapshot(rs.getBigDecimal("theoretical_cost_snapshot"))
                .quantity(rs.getBigDecimal("quantity"))
                .subtotalGrossSnapshot(rs.getBigDecimal("subtotal_gross_snapshot"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .taxes(new ArrayList<>())
                .build();
    }

    private OrderTaxDomain mapOrderTax(ResultSet rs, int rowNum) throws SQLException {
        return OrderTaxDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .orderId(rs.getObject("order_id", UUID.class))
                .taxId(rs.getObject("tax_id", UUID.class))
                .taxNameSnapshot(rs.getString("tax_name_snapshot"))
                .taxRateSnapshot(rs.getBigDecimal("tax_rate_snapshot"))
                .taxBaseSnapshot(rs.getBigDecimal("tax_base_snapshot"))
                .taxAmountSnapshot(rs.getBigDecimal("tax_amount_snapshot"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .build();
    }

    private OrderItemTaxDomain mapOrderItemTax(ResultSet rs, int rowNum) throws SQLException {
        return OrderItemTaxDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .orderItemId(rs.getObject("order_item_id", UUID.class))
                .taxId(rs.getObject("tax_id", UUID.class))
                .taxNameSnapshot(rs.getString("tax_name_snapshot"))
                .taxRateSnapshot(rs.getBigDecimal("tax_rate_snapshot"))
                .taxBaseSnapshot(rs.getBigDecimal("tax_base_snapshot"))
                .taxAmountSnapshot(rs.getBigDecimal("tax_amount_snapshot"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .build();
    }
}
