package com.beet.backend.modules.order.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.order.domain.model.*;
import com.beet.backend.modules.order.domain.spi.OrderBillPersistencePort;
import com.beet.backend.modules.order.domain.spi.OrderPersistencePort;
import com.beet.backend.modules.order.domain.spi.OrderTaxQueryPort;
import com.beet.backend.shared.domain.model.OperationMode;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderJdbcAdapter implements OrderPersistencePort, OrderBillPersistencePort, OrderTaxQueryPort {

    private static final Map<String, String> ORDER_SORTS = Map.ofEntries(
            Map.entry("createdAt", "o.created_at"),
            Map.entry("updatedAt", "o.updated_at"),
            Map.entry("total", "o.total_gross_snapshot"),
            Map.entry("customer", "LOWER(o.customer_name)"),
            Map.entry("orderStatus", "o.order_status"),
            Map.entry("paymentStatus", "o.payment_status"),
            Map.entry("kitchenStatus", "o.kitchen_status"),
            Map.entry("paymentExpiresAt", "o.payment_expires_at"));

    private static final Map<String, String> BILL_SORTS = Map.of(
            "createdAt", "b.created_at",
            "updatedAt", "b.updated_at",
            "total", "b.subtotal_gross_snapshot",
            "label", "LOWER(b.label)",
            "paymentStatus", "b.payment_status");

    private static final Map<String, String> POS_SORTS = Map.of(
            "menu", "LOWER(m.name), s.sort_order, LOWER(s.name), n.sort_order, LOWER(COALESCE(i.name, t.name))",
            "name", "LOWER(COALESCE(i.name, t.name))",
            "price", "COALESCE(i.sale_price, t.base_price)",
            "sortOrder", "m.name, s.sort_order, n.sort_order");

    private final JdbcClient jdbc;

    @Override
    public OrderDomain save(OrderDomain order) {
        UUID orderId = order.getId() != null ? order.getId() : UUID.randomUUID();
        order.setId(orderId);
        return jdbc.sql("""
                INSERT INTO orders
                    (id, restaurant_id, business_day_id, origin_cash_session_id, origin_device_id,
                     business_date, daily_sequence, public_code,
                     order_status, kitchen_status, payment_status, service_type, operation_mode_snapshot,
                     table_id, customer_name, customer_phone,
                     delivery_contact_name, delivery_phone, delivery_address, delivery_notes, delivery_fee, delivery_status,
                     prepayment_required_snapshot, tax_rate_snapshot, subtotal_gross_snapshot, tax_amount_snapshot,
                     total_gross_snapshot, tip_total_snapshot, refund_due_snapshot, refunded_total_snapshot,
                     payment_expires_at, payment_expired_at, expiration_processed_at,
                     notes, created_by, updated_by)
                VALUES
                    (:id, :restaurantId, :businessDayId, :originCashSessionId, :originDeviceId,
                     :businessDate, :dailySequence, :publicCode,
                     :orderStatus::order_status, :kitchenStatus::kitchen_status, :paymentStatus::payment_status,
                     :serviceType::service_type, :operationMode::operation_mode_enum,
                     :tableId, :customerName, :customerPhone,
                     :deliveryContactName, :deliveryPhone, :deliveryAddress, :deliveryNotes, :deliveryFee, :deliveryStatus::delivery_status,
                     :prepaymentRequired, :taxRate, :subtotalGross, :taxAmount,
                     :totalGross, :tipTotal, :refundDue, :refundedTotal,
                     :paymentExpiresAt, :paymentExpiredAt, :expirationProcessedAt,
                     :notes, :createdBy, :updatedBy)
                RETURNING *
                """)
                .params(orderParams(order))
                .query(this::mapOrder)
                .single();
    }

    @Override
    public OrderDomain update(OrderDomain order) {
        return jdbc.sql("""
                UPDATE orders
                   SET order_status = :orderStatus::order_status,
                       kitchen_status = :kitchenStatus::kitchen_status,
                       payment_status = :paymentStatus::payment_status,
                       service_type = :serviceType::service_type,
                       table_id = :tableId,
                       customer_name = :customerName,
                       customer_phone = :customerPhone,
                       delivery_contact_name = :deliveryContactName,
                       delivery_phone = :deliveryPhone,
                       delivery_address = :deliveryAddress,
                       delivery_notes = :deliveryNotes,
                       delivery_fee = :deliveryFee,
                       delivery_status = :deliveryStatus::delivery_status,
                       prepayment_required_snapshot = :prepaymentRequired,
                       tax_rate_snapshot = :taxRate,
                       subtotal_gross_snapshot = :subtotalGross,
                       tax_amount_snapshot = :taxAmount,
                       total_gross_snapshot = :totalGross,
                       tip_total_snapshot = :tipTotal,
                       refund_due_snapshot = :refundDue,
                       refunded_total_snapshot = :refundedTotal,
                       payment_expires_at = :paymentExpiresAt,
                       payment_expired_at = :paymentExpiredAt,
                       expiration_processed_at = :expirationProcessedAt,
                       notes = :notes,
                       canceled_at = CASE WHEN :orderStatus = 'CANCELED' THEN COALESCE(canceled_at, NOW()) ELSE canceled_at END,
                       completed_at = CASE WHEN :orderStatus = 'COMPLETED' THEN COALESCE(completed_at, NOW()) ELSE completed_at END,
                       cancel_reason = :cancelReason,
                       updated_at = NOW(),
                       updated_by = :updatedBy
                 WHERE id = :id
             RETURNING *
                """)
                .params(orderParams(order))
                .query(this::mapOrder)
                .single();
    }

    @Override
    public int nextDailySequence(UUID restaurantId, LocalDate businessDate) {
        Integer next = jdbc.sql("""
                INSERT INTO order_daily_sequences (restaurant_id, business_date, last_sequence)
                VALUES (:restaurantId, :businessDate, 1)
                ON CONFLICT (restaurant_id, business_date)
                DO UPDATE SET last_sequence = order_daily_sequences.last_sequence + 1,
                              updated_at = NOW()
                RETURNING last_sequence
                """)
                .param("restaurantId", restaurantId)
                .param("businessDate", businessDate)
                .query(Integer.class)
                .single();
        return next == null ? 1 : next;
    }

    @Override
    public boolean existsPublicCode(UUID restaurantId, String publicCode) {
        Boolean exists = jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM orders
                     WHERE restaurant_id = :restaurantId
                       AND public_code = :publicCode
                )
                """)
                .param("restaurantId", restaurantId)
                .param("publicCode", publicCode)
                .query(Boolean.class)
                .single();
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public OrderItemDomain saveItem(OrderItemDomain item) {
        UUID itemId = item.getId() != null ? item.getId() : UUID.randomUUID();
        item.setId(itemId);
        return jdbc.sql("""
                INSERT INTO order_items
                    (id, order_id, line_type, item_id, template_id, submenu_node_id,
                     item_name_snapshot, unit_price_snapshot, theoretical_cost_snapshot,
                     quantity, subtotal_gross_snapshot, notes, created_by, updated_by)
                VALUES
                    (:id, :orderId, :lineType::order_line_type, :itemId, :templateId, :submenuNodeId,
                     :itemName, :unitPrice, :theoreticalCost,
                     :quantity, :subtotalGross, :notes, :createdBy, :updatedBy)
                RETURNING *
                """)
                .param("id", itemId)
                .param("orderId", item.getOrderId())
                .param("lineType", item.getLineType().name())
                .param("itemId", item.getItemId())
                .param("templateId", item.getTemplateId())
                .param("submenuNodeId", item.getSubmenuNodeId())
                .param("itemName", item.getItemNameSnapshot())
                .param("unitPrice", item.getUnitPriceSnapshot())
                .param("theoreticalCost", item.getTheoreticalCostSnapshot())
                .param("quantity", item.getQuantity())
                .param("subtotalGross", item.getSubtotalGrossSnapshot())
                .param("notes", item.getNotes())
                .param("createdBy", item.getCreatedBy())
                .param("updatedBy", item.getUpdatedBy())
                .query(this::mapOrderItem)
                .single();
    }

    @Override
    public void saveTemplateSnapshots(OrderItemDomain item) {
        if (item.getTemplateSlots() == null || item.getTemplateSlots().isEmpty()) {
            return;
        }
        for (OrderItemTemplateSlotDomain slot : item.getTemplateSlots()) {
            UUID slotSnapshotId = UUID.randomUUID();
            slot.setId(slotSnapshotId);
            slot.setOrderItemId(item.getId());
            jdbc.sql("""
                    INSERT INTO order_item_template_slots
                        (id, order_item_id, template_slot_id, slot_name_snapshot,
                         min_selection_snapshot, max_selection_snapshot, sort_order)
                    VALUES (:id, :orderItemId, :templateSlotId, :name, :minSelection, :maxSelection, :sortOrder)
                    """)
                    .param("id", slotSnapshotId)
                    .param("orderItemId", item.getId())
                    .param("templateSlotId", slot.getTemplateSlotId())
                    .param("name", slot.getSlotNameSnapshot())
                    .param("minSelection", slot.getMinSelectionSnapshot())
                    .param("maxSelection", slot.getMaxSelectionSnapshot())
                    .param("sortOrder", slot.getSortOrder())
                    .update();
            for (OrderItemTemplateOptionDomain option : slot.getOptions()) {
                UUID optionSnapshotId = UUID.randomUUID();
                option.setId(optionSnapshotId);
                option.setOrderItemTemplateSlotId(slotSnapshotId);
                jdbc.sql("""
                        INSERT INTO order_item_template_options
                            (id, order_item_template_slot_id, slot_option_id, item_id, item_name_snapshot,
                             quantity, surcharge_snapshot, theoretical_cost_snapshot)
                        VALUES (:id, :slotId, :slotOptionId, :itemId, :itemName, :quantity, :surcharge, :cost)
                        """)
                        .param("id", optionSnapshotId)
                        .param("slotId", slotSnapshotId)
                        .param("slotOptionId", option.getSlotOptionId())
                        .param("itemId", option.getItemId())
                        .param("itemName", option.getItemNameSnapshot())
                        .param("quantity", option.getQuantity())
                        .param("surcharge", option.getSurchargeSnapshot())
                        .param("cost", option.getTheoreticalCostSnapshot())
                        .update();
            }
        }
    }

    @Override
    public void saveIngredientRequirements(OrderItemDomain item) {
        for (OrderItemIngredientRequirementDomain requirement :
                item.getIngredientRequirements() == null
                        ? List.<OrderItemIngredientRequirementDomain>of()
                        : item.getIngredientRequirements()) {
            UUID id = UUID.randomUUID();
            requirement.setId(id);
            requirement.setOrderId(item.getOrderId());
            requirement.setOrderItemId(item.getId());
            jdbc.sql("""
                    INSERT INTO order_item_ingredient_requirements
                        (id, restaurant_id, order_id, order_item_id, master_ingredient_id,
                         ingredient_name_snapshot, quantity_base_per_sale_unit, unit_cost_snapshot)
                    VALUES
                        (:id, :restaurantId, :orderId, :orderItemId, :ingredientId,
                         :ingredientName, :quantity, :unitCost)
                    """)
                    .param("id", id)
                    .param("restaurantId", requirement.getRestaurantId())
                    .param("orderId", item.getOrderId())
                    .param("orderItemId", item.getId())
                    .param("ingredientId", requirement.getMasterIngredientId())
                    .param("ingredientName", requirement.getIngredientNameSnapshot())
                    .param("quantity", requirement.getQuantityBasePerSaleUnit())
                    .param("unitCost", requirement.getUnitCostSnapshot())
                    .update();
        }
    }

    @Override
    public List<OrderItemIngredientRequirementDomain> findIngredientRequirements(UUID orderItemId) {
        return jdbc.sql("""
                SELECT *
                  FROM order_item_ingredient_requirements
                 WHERE order_item_id = :orderItemId
                 ORDER BY ingredient_name_snapshot, master_ingredient_id
                """)
                .param("orderItemId", orderItemId)
                .query(this::mapIngredientRequirement)
                .list();
    }

    @Override
    public Optional<IngredientStockAvailabilityDomain> findIngredientStockAvailability(
            UUID restaurantId, UUID masterIngredientId) {
        return jdbc.sql("""
                SELECT s.master_ingredient_id,
                       s.current_stock,
                       s.min_stock,
                       COALESCE(SUM(r.quantity_base) FILTER (WHERE r.status = 'ACTIVE'), 0) AS active_reserved
                  FROM ingredient_stocks s
             LEFT JOIN inventory_reservations r ON r.ingredient_stock_id = s.id
                 WHERE s.restaurant_id = :restaurantId
                   AND s.master_ingredient_id = :ingredientId
                   AND s.deleted_at IS NULL
                 GROUP BY s.master_ingredient_id, s.current_stock, s.min_stock
                """)
                .param("restaurantId", restaurantId)
                .param("ingredientId", masterIngredientId)
                .query((rs, rowNum) -> new IngredientStockAvailabilityDomain(
                        rs.getObject("master_ingredient_id", UUID.class),
                        rs.getBigDecimal("current_stock"),
                        rs.getBigDecimal("min_stock"),
                        rs.getBigDecimal("active_reserved")))
                .optional();
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
    public OrderItemCancellationDomain applyItemCancellation(
            OrderItemCancellationDomain cancellation,
            BigDecimal originalQuantity,
            BigDecimal activeSubtotal,
            UUID userId) {
        UUID cancellationId = cancellation.getId() != null ? cancellation.getId() : UUID.randomUUID();
        cancellation.setId(cancellationId);
        OrderItemCancellationDomain saved = jdbc.sql("""
                INSERT INTO order_item_cancellations
                    (id, restaurant_id, order_id, order_item_id, quantity, gross_amount, reason,
                     kitchen_status_snapshot, inventory_disposition, created_by, system_generated)
                VALUES
                    (:id, :restaurantId, :orderId, :orderItemId, :quantity, :grossAmount, :reason,
                     :kitchenStatus::kitchen_status,
                     :inventoryDisposition::order_item_inventory_disposition, :createdBy, :systemGenerated)
                RETURNING *
                """)
                .param("id", cancellationId)
                .param("restaurantId", cancellation.getRestaurantId())
                .param("orderId", cancellation.getOrderId())
                .param("orderItemId", cancellation.getOrderItemId())
                .param("quantity", cancellation.getQuantity())
                .param("grossAmount", cancellation.getGrossAmount())
                .param("reason", cancellation.getReason())
                .param("kitchenStatus", cancellation.getKitchenStatusSnapshot().name())
                .param("inventoryDisposition", cancellation.getInventoryDisposition().name())
                .param("createdBy", cancellation.isSystemGenerated() ? null : userId)
                .param("systemGenerated", cancellation.isSystemGenerated())
                .query(this::mapItemCancellation)
                .single();

        List<OrderItemCancellationInventoryDomain> inventoryEntries =
                cancellation.getInventoryDisposition() == OrderItemInventoryDisposition.RELEASE_RESERVED
                        ? releaseReservationQuantities(cancellationId, cancellation, userId)
                        : recordConsumedCancellation(cancellationId, cancellation, userId);

        jdbc.sql("""
                UPDATE order_items
                   SET canceled_quantity = canceled_quantity + :quantity,
                       subtotal_gross_snapshot = :activeSubtotal,
                       updated_at = NOW(),
                       updated_by = :userId
                 WHERE id = :orderItemId
                   AND canceled_quantity + :quantity <= quantity
                """)
                .param("quantity", cancellation.getQuantity())
                .param("activeSubtotal", activeSubtotal)
                .param("userId", userId)
                .param("orderItemId", cancellation.getOrderItemId())
                .update();

        cancelKitchenTicketLines(cancellation.getOrderItemId(), cancellation.getQuantity());
        saved.setInventoryEntries(inventoryEntries);
        return saved;
    }

    @Override
    public void cancelEmptyKitchenTickets(UUID orderId, UUID userId) {
        jdbc.sql("""
                UPDATE kitchen_tickets kt
                   SET status = 'CANCELED',
                       canceled_at = COALESCE(canceled_at, NOW()),
                       canceled_by = COALESCE(canceled_by, :userId)
                 WHERE kt.order_id = :orderId
                   AND kt.status <> 'CANCELED'
                   AND NOT EXISTS (
                       SELECT 1
                         FROM kitchen_ticket_lines l
                        WHERE l.kitchen_ticket_id = kt.id
                          AND l.quantity > l.canceled_quantity
                   )
                """)
                .param("orderId", orderId)
                .param("userId", userId)
                .update();
    }

    @Override
    public void replaceOrderTaxes(UUID orderId, List<OrderTaxDomain> taxes) {
        jdbc.sql("DELETE FROM order_taxes WHERE order_id = :orderId")
                .param("orderId", orderId)
                .update();
        for (OrderTaxDomain tax : taxes == null ? List.<OrderTaxDomain>of() : taxes) {
            UUID id = tax.getId() != null ? tax.getId() : UUID.randomUUID();
            jdbc.sql("""
                    INSERT INTO order_taxes
                        (id, order_id, tax_id, tax_name_snapshot, tax_rate_snapshot,
                         tax_base_snapshot, tax_amount_snapshot, created_by)
                    VALUES (:id, :orderId, :taxId, :name, :rate, :base, :amount, :createdBy)
                    """)
                    .param("id", id)
                    .param("orderId", orderId)
                    .param("taxId", tax.getTaxId())
                    .param("name", tax.getTaxNameSnapshot())
                    .param("rate", tax.getTaxRateSnapshot())
                    .param("base", tax.getTaxBaseSnapshot())
                    .param("amount", tax.getTaxAmountSnapshot())
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
        for (OrderItemTaxDomain tax : taxes == null ? List.<OrderItemTaxDomain>of() : taxes) {
            UUID id = tax.getId() != null ? tax.getId() : UUID.randomUUID();
            jdbc.sql("""
                    INSERT INTO order_item_taxes
                        (id, order_item_id, tax_id, tax_name_snapshot, tax_rate_snapshot,
                         tax_base_snapshot, tax_amount_snapshot, created_by)
                    VALUES (:id, :orderItemId, :taxId, :name, :rate, :base, :amount, :createdBy)
                    """)
                    .param("id", id)
                    .param("orderItemId", tax.getOrderItemId())
                    .param("taxId", tax.getTaxId())
                    .param("name", tax.getTaxNameSnapshot())
                    .param("rate", tax.getTaxRateSnapshot())
                    .param("base", tax.getTaxBaseSnapshot())
                    .param("amount", tax.getTaxAmountSnapshot())
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
                SELECT * FROM order_items WHERE order_id = :orderId ORDER BY created_at ASC, id ASC
                """)
                .param("orderId", orderId)
                .query(this::mapOrderItem)
                .list();
        hydrateTemplateSnapshots(items);
        items.forEach(item -> item.setIngredientRequirements(findIngredientRequirements(item.getId())));
        items.forEach(item -> item.setCancellations(findItemCancellations(item.getId())));
        hydrateTaxes(orderId, domain, items);
        domain.setKitchenTickets(findTicketsByOrder(orderId));
        domain.setPayments(findPaymentsByOrder(orderId));
        domain.setRefunds(findRefundsByOrder(domain.getRestaurantId(), orderId));
        domain.setItems(items);
        return Optional.of(domain);
    }

    @Override
    public PageResponse<OrderDomain> findAllPaged(OrderSearchCriteria criteria) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder(" FROM orders o WHERE o.restaurant_id = :restaurantId ");
        params.put("restaurantId", criteria.restaurantId());
        appendOrderFilters(where, params, criteria);
        String orderClause = resolveSort(criteria.sort(), ORDER_SORTS, "o.created_at DESC, o.id DESC");
        return queryOrders(where, params, orderClause, criteria.page(), criteria.size());
    }

    @Override
    public PageResponse<OrderDomain> findAllPaged(UUID restaurantId, int page, int size, String search) {
        return findAllPaged(new OrderSearchCriteria(
                restaurantId, page, size,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null,
                search));
    }

    @Override
    public PageResponse<PosCatalogEntryDomain> findPosCatalog(UUID restaurantId, int page, int size, String search,
            UUID menuId, UUID submenuId, String referenceType, String sort) {
        Map<String, Object> params = new HashMap<>();
        params.put("restaurantId", restaurantId);
        StringBuilder base = new StringBuilder("""
                FROM submenu_nodes n
                JOIN submenus s ON s.id = n.submenu_id AND s.restaurant_id = n.restaurant_id
                JOIN menus m ON m.id = s.menu_id AND m.restaurant_id = s.restaurant_id
                LEFT JOIN items i ON i.id = n.item_id AND i.restaurant_id = n.restaurant_id
                LEFT JOIN templates t ON t.id = n.template_id AND t.restaurant_id = n.restaurant_id
                LEFT JOIN catalog_availability_overrides o
                  ON o.restaurant_id = n.restaurant_id
                 AND o.reference_type::text = n.node_type::text
                 AND o.reference_id = COALESCE(n.item_id, n.template_id)
                 AND (o.expires_at IS NULL OR o.expires_at > NOW())
                WHERE n.restaurant_id = :restaurantId
                  AND (
                    (n.node_type = 'PRODUCT' AND i.is_active = true AND i.deleted_at IS NULL)
                    OR
                    (n.node_type = 'TEMPLATE' AND t.is_active = true AND t.deleted_at IS NULL)
                  )
                """);
        if (menuId != null) {
            base.append(" AND m.id = :menuId ");
            params.put("menuId", menuId);
        }
        if (submenuId != null) {
            base.append(" AND s.id = :submenuId ");
            params.put("submenuId", submenuId);
        }
        if (referenceType != null && !referenceType.isBlank()) {
            try {
                CatalogReferenceType type = CatalogReferenceType.valueOf(referenceType.trim().toUpperCase());
                base.append(" AND n.node_type = :referenceType::submenu_node_type ");
                params.put("referenceType", type.name());
            } catch (IllegalArgumentException ignored) {
                base.append(" AND 1 = 0 ");
            }
        }
        if (search != null && !search.isBlank()) {
            base.append("""
                     AND (
                        LOWER(COALESCE(i.name, t.name)) LIKE :search
                        OR LOWER(COALESCE(i.description, t.description, '')) LIKE :search
                        OR LOWER(m.name) LIKE :search
                        OR LOWER(s.name) LIKE :search
                     )
                    """);
            params.put("search", "%" + search.toLowerCase() + "%");
        }

        Long total = jdbc.sql("SELECT COUNT(*) " + base)
                .params(params)
                .query(Long.class)
                .single();
        List<PosCatalogEntryDomain> content = jdbc.sql("""
                SELECT n.id AS node_id, m.id AS menu_id, m.name AS menu_name,
                       s.id AS submenu_id, s.name AS submenu_name,
                       n.node_type, COALESCE(n.item_id, n.template_id) AS reference_id,
                       COALESCE(i.name, t.name) AS name,
                       COALESCE(i.description, t.description) AS description,
                       COALESCE(i.sale_price, t.base_price) AS price,
                       n.sort_order,
                       COALESCE(o.status::text = 'UNAVAILABLE', false) AS manually_unavailable
                """ + base + " ORDER BY "
                        + resolveSort(sort, POS_SORTS, "LOWER(m.name) ASC, s.sort_order ASC, n.sort_order ASC, name ASC")
                        + " LIMIT :size OFFSET :offset")
                .params(withPage(params, page, size))
                .query(this::mapPosCatalogEntry)
                .list();
        content.forEach(entry -> {
            if (entry.getReferenceType() == CatalogReferenceType.TEMPLATE) {
                entry.setSlots(findPosTemplateSlots(entry.getReferenceId()));
            }
        });
        return PageResponse.of(content, total == null ? 0 : total, page, size);
    }

    @Override
    public KitchenTicketDomain saveKitchenTicket(KitchenTicketDomain ticket) {
        UUID id = ticket.getId() != null ? ticket.getId() : UUID.randomUUID();
        ticket.setId(id);
        KitchenTicketDomain saved = jdbc.sql("""
                INSERT INTO kitchen_tickets (id, restaurant_id, order_id, status, sent_by, notes)
                VALUES (:id, :restaurantId, :orderId, :status::kitchen_ticket_status, :sentBy, :notes)
                RETURNING *
                """)
                .param("id", id)
                .param("restaurantId", ticket.getRestaurantId())
                .param("orderId", ticket.getOrderId())
                .param("status", ticket.getStatus().name())
                .param("sentBy", ticket.getSentBy())
                .param("notes", ticket.getNotes())
                .query(this::mapKitchenTicket)
                .single();
        List<KitchenTicketLineDomain> lines = new ArrayList<>();
        for (KitchenTicketLineDomain line : ticket.getLines()) {
            UUID lineId = UUID.randomUUID();
            lines.add(jdbc.sql("""
                    INSERT INTO kitchen_ticket_lines
                        (id, kitchen_ticket_id, order_item_id, quantity, item_name_snapshot, notes)
                    VALUES (:id, :ticketId, :orderItemId, :quantity, :name, :notes)
                    RETURNING *
                    """)
                    .param("id", lineId)
                    .param("ticketId", id)
                    .param("orderItemId", line.getOrderItemId())
                    .param("quantity", line.getQuantity())
                    .param("name", line.getItemNameSnapshot())
                    .param("notes", line.getNotes())
                    .query(this::mapKitchenTicketLine)
                    .single());
        }
        saved.setLines(lines);
        return saved;
    }

    @Override
    public Optional<KitchenTicketDomain> findKitchenTicket(UUID restaurantId, UUID ticketId) {
        Optional<KitchenTicketDomain> ticket = jdbc.sql("""
                SELECT kt.*,
                       o.business_date AS order_business_date,
                       o.daily_sequence AS order_daily_sequence,
                       o.public_code AS order_public_code,
                       o.customer_name AS order_customer_name
                  FROM kitchen_tickets kt
                  JOIN orders o ON o.id = kt.order_id
                 WHERE kt.restaurant_id = :restaurantId AND kt.id = :ticketId
                """)
                .param("restaurantId", restaurantId)
                .param("ticketId", ticketId)
                .query(this::mapKitchenTicket)
                .optional();
        ticket.ifPresent(t -> t.setLines(findTicketLines(t.getId())));
        return ticket;
    }

    @Override
    public PageResponse<KitchenTicketDomain> findKitchenTickets(UUID restaurantId, KitchenTicketStatus status,
            int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = safePage * safeSize;
        String statusFilter = status != null ? " AND kt.status = :status::kitchen_ticket_status" : "";
        String dataSql = """
                SELECT kt.*,
                       o.business_date AS order_business_date,
                       o.daily_sequence AS order_daily_sequence,
                       o.public_code AS order_public_code,
                       o.customer_name AS order_customer_name
                  FROM kitchen_tickets kt
                  JOIN orders o ON o.id = kt.order_id
                 WHERE kt.restaurant_id = :restaurantId
                """
                + statusFilter + " ORDER BY kt.sent_at DESC LIMIT :limit OFFSET :offset";
        String countSql = "SELECT COUNT(*) FROM kitchen_tickets kt WHERE kt.restaurant_id = :restaurantId" + statusFilter;

        var dataSpec = jdbc.sql(dataSql)
                .param("restaurantId", restaurantId)
                .param("limit", safeSize)
                .param("offset", offset);
        var countSpec = jdbc.sql(countSql)
                .param("restaurantId", restaurantId);
        if (status != null) {
            dataSpec = dataSpec.param("status", status.name());
            countSpec = countSpec.param("status", status.name());
        }

        List<KitchenTicketDomain> tickets = dataSpec.query(this::mapKitchenTicket).list();
        tickets.forEach(ticket -> ticket.setLines(findTicketLines(ticket.getId())));
        Long total = countSpec.query(Long.class).single();
        return PageResponse.of(tickets, total, safePage, safeSize);
    }

    @Override
    public KitchenTicketDomain updateKitchenTicketStatus(UUID restaurantId, UUID ticketId, KitchenTicketStatus status,
            UUID userId) {
        KitchenTicketDomain ticket = jdbc.sql("""
                WITH updated AS (
                    UPDATE kitchen_tickets
                       SET status = :status::kitchen_ticket_status,
                           started_at = CASE WHEN :status = 'PREPARING' THEN COALESCE(started_at, NOW()) ELSE started_at END,
                           started_by = CASE WHEN :status = 'PREPARING' THEN COALESCE(started_by, :userId) ELSE started_by END,
                           ready_at = CASE WHEN :status = 'READY' THEN COALESCE(ready_at, NOW()) ELSE ready_at END,
                           ready_by = CASE WHEN :status = 'READY' THEN COALESCE(ready_by, :userId) ELSE ready_by END,
                           canceled_at = CASE WHEN :status = 'CANCELED' THEN COALESCE(canceled_at, NOW()) ELSE canceled_at END,
                           canceled_by = CASE WHEN :status = 'CANCELED' THEN COALESCE(canceled_by, :userId) ELSE canceled_by END
                     WHERE id = :ticketId AND restaurant_id = :restaurantId
                 RETURNING *
                )
                SELECT u.*,
                       o.business_date AS order_business_date,
                       o.daily_sequence AS order_daily_sequence,
                       o.public_code AS order_public_code,
                       o.customer_name AS order_customer_name
                  FROM updated u
                  JOIN orders o ON o.id = u.order_id
                """)
                .param("status", status.name())
                .param("userId", userId)
                .param("ticketId", ticketId)
                .param("restaurantId", restaurantId)
                .query(this::mapKitchenTicket)
                .single();
        ticket.setLines(findTicketLines(ticketId));
        return ticket;
    }

    @Override
    public List<InventoryReservationDomain> findActiveReservationsByOrderItem(UUID orderItemId) {
        return jdbc.sql("""
                SELECT * FROM inventory_reservations
                 WHERE order_item_id = :orderItemId AND status = 'ACTIVE'
                """)
                .param("orderItemId", orderItemId)
                .query(this::mapReservation)
                .list();
    }

    @Override
    public void saveReservations(List<InventoryReservationDomain> reservations) {
        for (InventoryReservationDomain reservation : reservations) {
            StockLock stock = lockStock(reservation.getRestaurantId(), reservation.getMasterIngredientId());
            BigDecimal reserved = activeReservedQuantity(stock.stockId());
            BigDecimal available = stock.currentStock().subtract(reserved);
            if (available.compareTo(reservation.getQuantityBase()) < 0) {
                throw new IllegalArgumentException("Insufficient stock for ingredient: " + reservation.getMasterIngredientId());
            }
            UUID id = UUID.randomUUID();
            reservation.setId(id);
            reservation.setIngredientStockId(stock.stockId());
            jdbc.sql("""
                    INSERT INTO inventory_reservations
                        (id, restaurant_id, order_id, order_item_id, ingredient_stock_id, master_ingredient_id,
                         quantity_base, unit_cost_snapshot, status, created_by, updated_by)
                    VALUES
                        (:id, :restaurantId, :orderId, :orderItemId, :stockId, :ingredientId,
                         :quantity, :unitCost, :status::inventory_reservation_status, :createdBy, :updatedBy)
                    """)
                    .param("id", id)
                    .param("restaurantId", reservation.getRestaurantId())
                    .param("orderId", reservation.getOrderId())
                    .param("orderItemId", reservation.getOrderItemId())
                    .param("stockId", stock.stockId())
                    .param("ingredientId", reservation.getMasterIngredientId())
                    .param("quantity", reservation.getQuantityBase())
                    .param("unitCost", reservation.getUnitCostSnapshot())
                    .param("status", reservation.getStatus().name())
                    .param("createdBy", reservation.getCreatedBy())
                    .param("updatedBy", reservation.getUpdatedBy())
                    .update();
        }
    }

    @Override
    public void releaseReservationsByOrderItem(UUID orderItemId, UUID userId) {
        jdbc.sql("""
                UPDATE inventory_reservations
                   SET status = 'RELEASED', updated_at = NOW(), updated_by = :userId
                 WHERE order_item_id = :orderItemId AND status = 'ACTIVE'
                """)
                .param("orderItemId", orderItemId)
                .param("userId", userId)
                .update();
    }

    @Override
    public void releaseReservationsByOrder(UUID orderId, UUID userId) {
        jdbc.sql("""
                UPDATE inventory_reservations
                   SET status = 'RELEASED', updated_at = NOW(), updated_by = :userId
                 WHERE order_id = :orderId AND status = 'ACTIVE'
                """)
                .param("orderId", orderId)
                .param("userId", userId)
                .update();
    }

    @Override
    public List<OrderDomain> lockExpiredAwaitingPayments(OffsetDateTime now, int limit) {
        return jdbc.sql("""
                SELECT o.*
                  FROM orders o
                 WHERE o.order_status = 'AWAITING_PAYMENT'
                   AND o.payment_expires_at <= :now
                   AND o.expiration_processed_at IS NULL
                 ORDER BY o.payment_expires_at, o.id
                 LIMIT :limit
                 FOR UPDATE SKIP LOCKED
                """)
                .param("now", now)
                .param("limit", limit)
                .query(this::mapOrder)
                .list();
    }

    @Override
    public void lockOrder(UUID restaurantId, UUID orderId) {
        jdbc.sql("""
                SELECT id
                  FROM orders
                 WHERE id = :orderId
                   AND restaurant_id = :restaurantId
                 FOR UPDATE
                """)
                .param("orderId", orderId)
                .param("restaurantId", restaurantId)
                .query(UUID.class)
                .optional();
    }

    @Override
    public void consumeReservationsByTicket(UUID restaurantId, UUID ticketId, UUID userId) {
        List<InventoryReservationDomain> reservations = jdbc.sql("""
                SELECT r.*
                  FROM inventory_reservations r
                  JOIN kitchen_ticket_lines l ON l.order_item_id = r.order_item_id
                  JOIN kitchen_tickets t ON t.id = l.kitchen_ticket_id
                 WHERE t.id = :ticketId AND t.restaurant_id = :restaurantId AND r.status = 'ACTIVE'
                """)
                .param("ticketId", ticketId)
                .param("restaurantId", restaurantId)
                .query(this::mapReservation)
                .list();
        for (InventoryReservationDomain reservation : reservations) {
            StockLock stock = lockStockById(reservation.getIngredientStockId());
            BigDecimal resulting = stock.currentStock().subtract(reservation.getQuantityBase());
            if (resulting.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Insufficient stock while consuming reservation.");
            }
            jdbc.sql("""
                    UPDATE ingredient_stocks
                       SET current_stock = :resulting, updated_at = NOW(), updated_by = :userId
                     WHERE id = :stockId
                    """)
                    .param("resulting", resulting)
                    .param("userId", userId)
                    .param("stockId", stock.stockId())
                    .update();
            UUID txId = UUID.randomUUID();
            jdbc.sql("""
                    INSERT INTO inventory_transactions
                        (id, ingredient_stock_id, delta, reason, invoice_id, order_id, order_item_id,
                         previous_stock, resulting_stock, notes, created_by)
                    VALUES
                        (:id, :stockId, :delta, 'SALE', NULL, :orderId, :orderItemId,
                         :previous, :resulting, :notes, :createdBy)
                    """)
                    .param("id", txId)
                    .param("stockId", stock.stockId())
                    .param("delta", reservation.getQuantityBase().negate())
                    .param("orderId", reservation.getOrderId())
                    .param("orderItemId", reservation.getOrderItemId())
                    .param("previous", stock.currentStock())
                    .param("resulting", resulting)
                    .param("notes", "Sale consumption")
                    .param("createdBy", userId)
                    .update();
            jdbc.sql("""
                    INSERT INTO order_item_consumptions
                        (restaurant_id, order_id, order_item_id, ingredient_stock_id, master_ingredient_id,
                         quantity_base, unit_cost_snapshot, inventory_transaction_id, created_by)
                    VALUES
                        (:restaurantId, :orderId, :orderItemId, :stockId, :ingredientId,
                         :quantity, :unitCost, :txId, :createdBy)
                    """)
                    .param("restaurantId", reservation.getRestaurantId())
                    .param("orderId", reservation.getOrderId())
                    .param("orderItemId", reservation.getOrderItemId())
                    .param("stockId", reservation.getIngredientStockId())
                    .param("ingredientId", reservation.getMasterIngredientId())
                    .param("quantity", reservation.getQuantityBase())
                    .param("unitCost", reservation.getUnitCostSnapshot())
                    .param("txId", txId)
                    .param("createdBy", userId)
                    .update();
            jdbc.sql("""
                    UPDATE inventory_reservations
                       SET status = 'CONSUMED', updated_at = NOW(), updated_by = :userId
                     WHERE id = :id
                    """)
                    .param("id", reservation.getId())
                    .param("userId", userId)
                    .update();
        }
    }

    @Override
    public List<PaymentMethodDomain> findPaymentMethods(UUID restaurantId) {
        return jdbc.sql("""
                SELECT * FROM payment_methods
                 WHERE restaurant_id = :restaurantId
                 ORDER BY sort_order ASC, name ASC
                """)
                .param("restaurantId", restaurantId)
                .query(this::mapPaymentMethod)
                .list();
    }

    @Override
    public PaymentMethodDomain savePaymentMethod(PaymentMethodDomain method) {
        UUID id = method.getId() != null ? method.getId() : UUID.randomUUID();
        method.setId(id);
        return jdbc.sql("""
                INSERT INTO payment_methods
                    (id, restaurant_id, code, name, type, is_active, requires_reference,
                     sort_order, created_by, updated_by)
                VALUES
                    (:id, :restaurantId, :code, :name, :type::payment_method_type, :active, :requiresReference,
                     :sortOrder, :createdBy, :updatedBy)
                RETURNING *
                """)
                .param("id", id)
                .param("restaurantId", method.getRestaurantId())
                .param("code", method.getCode())
                .param("name", method.getName())
                .param("type", method.getType().name())
                .param("active", method.isActive())
                .param("requiresReference", method.isRequiresReference())
                .param("sortOrder", method.getSortOrder())
                .param("createdBy", method.getCreatedBy())
                .param("updatedBy", method.getUpdatedBy())
                .query(this::mapPaymentMethod)
                .single();
    }

    @Override
    public boolean existsPaymentMethodCode(UUID restaurantId, String code) {
        Boolean exists = jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM payment_methods
                     WHERE restaurant_id = :restaurantId AND code = :code
                )
                """)
                .param("restaurantId", restaurantId)
                .param("code", code)
                .query(Boolean.class)
                .single();
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public int countActivePaymentMethods(UUID restaurantId) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM payment_methods
                 WHERE restaurant_id = :restaurantId AND is_active = TRUE
                """)
                .param("restaurantId", restaurantId)
                .query(Integer.class)
                .single();
    }

    @Override
    public PaymentMethodDomain updatePaymentMethod(PaymentMethodDomain method) {
        return jdbc.sql("""
                UPDATE payment_methods
                   SET name = :name,
                       is_active = :active,
                       requires_reference = :requiresReference,
                       sort_order = :sortOrder,
                       updated_at = NOW(),
                       updated_by = :updatedBy
                 WHERE id = :id AND restaurant_id = :restaurantId
             RETURNING *
                """)
                .param("id", method.getId())
                .param("restaurantId", method.getRestaurantId())
                .param("name", method.getName())
                .param("active", method.isActive())
                .param("requiresReference", method.isRequiresReference())
                .param("sortOrder", method.getSortOrder())
                .param("updatedBy", method.getUpdatedBy())
                .query(this::mapPaymentMethod)
                .single();
    }

    @Override
    public Optional<PaymentMethodDomain> findPaymentMethod(UUID restaurantId, UUID methodId) {
        return jdbc.sql("""
                SELECT * FROM payment_methods WHERE restaurant_id = :restaurantId AND id = :id
                """)
                .param("restaurantId", restaurantId)
                .param("id", methodId)
                .query(this::mapPaymentMethod)
                .optional();
    }

    @Override
    public PaymentDomain savePayment(PaymentDomain payment) {
        UUID id = payment.getId() != null ? payment.getId() : UUID.randomUUID();
        payment.setId(id);
        return jdbc.sql("""
                INSERT INTO payments
                    (id, restaurant_id, order_id, order_bill_id, payment_method_id, cash_session_id,
                     device_id, amount, tip_amount, status, external_reference, notes, created_by)
                VALUES
                    (:id, :restaurantId, :orderId, :billId, :methodId, :cashSessionId,
                     :deviceId, :amount, :tip, :status::payment_record_status, :reference, :notes, :createdBy)
                RETURNING *
                """)
                .param("id", id)
                .param("restaurantId", payment.getRestaurantId())
                .param("orderId", payment.getOrderId())
                .param("billId", payment.getOrderBillId())
                .param("methodId", payment.getPaymentMethodId())
                .param("cashSessionId", payment.getCashSessionId())
                .param("deviceId", payment.getDeviceId())
                .param("amount", payment.getAmount())
                .param("tip", payment.getTipAmount() == null ? BigDecimal.ZERO : payment.getTipAmount())
                .param("status", payment.getStatus().name())
                .param("reference", payment.getExternalReference())
                .param("notes", payment.getNotes())
                .param("createdBy", payment.getCreatedBy())
                .query(this::mapPayment)
                .single();
    }

    @Override
    public Optional<PaymentDomain> findPayment(UUID restaurantId, UUID orderId, UUID paymentId) {
        return jdbc.sql("""
                SELECT *
                  FROM payments
                 WHERE id = :paymentId
                   AND restaurant_id = :restaurantId
                   AND order_id = :orderId
                """)
                .param("paymentId", paymentId)
                .param("restaurantId", restaurantId)
                .param("orderId", orderId)
                .query(this::mapPayment)
                .optional();
    }

    @Override
    public BigDecimal sumRecordedPayments(UUID orderId) {
        return sumPaymentColumn(orderId, "amount");
    }

    @Override
    public BigDecimal sumRecordedTips(UUID orderId) {
        return sumPaymentColumn(orderId, "tip_amount");
    }

    @Override
    public PaymentRefundDomain saveRefund(PaymentRefundDomain refund) {
        UUID id = refund.getId() != null ? refund.getId() : UUID.randomUUID();
        refund.setId(id);
        return jdbc.sql("""
                INSERT INTO payment_refunds
                    (id, restaurant_id, order_id, payment_id, cash_session_id, device_id,
                     amount, status, reason, external_reference, created_by)
                VALUES
                    (:id, :restaurantId, :orderId, :paymentId, :cashSessionId, :deviceId,
                     :amount, :status::payment_refund_status, :reason, :reference, :createdBy)
                RETURNING *
                """)
                .param("id", id)
                .param("restaurantId", refund.getRestaurantId())
                .param("orderId", refund.getOrderId())
                .param("paymentId", refund.getPaymentId())
                .param("cashSessionId", refund.getCashSessionId())
                .param("deviceId", refund.getDeviceId())
                .param("amount", refund.getAmount())
                .param("status", refund.getStatus().name())
                .param("reason", refund.getReason())
                .param("reference", refund.getExternalReference())
                .param("createdBy", refund.getCreatedBy())
                .query(this::mapRefund)
                .single();
    }

    @Override
    public List<PaymentRefundDomain> findRefundsByOrder(UUID restaurantId, UUID orderId) {
        return jdbc.sql("""
                SELECT *
                  FROM payment_refunds
                 WHERE restaurant_id = :restaurantId
                   AND order_id = :orderId
                 ORDER BY created_at ASC
                """)
                .param("restaurantId", restaurantId)
                .param("orderId", orderId)
                .query(this::mapRefund)
                .list();
    }

    @Override
    public BigDecimal sumRecordedRefunds(UUID orderId) {
        return jdbc.sql("""
                SELECT COALESCE(SUM(amount), 0)
                  FROM payment_refunds
                 WHERE order_id = :orderId
                   AND status = 'RECORDED'
                """)
                .param("orderId", orderId)
                .query(BigDecimal.class)
                .single();
    }

    @Override
    public BigDecimal sumRecordedRefundsByPayment(UUID paymentId) {
        return jdbc.sql("""
                SELECT COALESCE(SUM(amount), 0)
                  FROM payment_refunds
                 WHERE payment_id = :paymentId
                   AND status = 'RECORDED'
                """)
                .param("paymentId", paymentId)
                .query(BigDecimal.class)
                .single();
    }

    @Override
    public void deleteBillsByOrder(UUID orderId) {
        jdbc.sql("DELETE FROM order_bills WHERE order_id = :orderId")
                .param("orderId", orderId)
                .update();
    }

    @Override
    public List<OrderBillDomain> saveBills(List<OrderBillDomain> bills) {
        List<OrderBillDomain> saved = new ArrayList<>();
        for (OrderBillDomain bill : bills) {
            UUID id = UUID.randomUUID();
            bill.setId(id);
            OrderBillDomain persisted = jdbc.sql("""
                    INSERT INTO order_bills
                        (id, restaurant_id, order_id, label, split_mode, subtotal_gross_snapshot,
                         tip_total_snapshot, total_paid_snapshot, payment_status, created_by, updated_by)
                    VALUES
                        (:id, :restaurantId, :orderId, :label, :splitMode::bill_split_mode, :subtotal,
                         :tip, :paid, :paymentStatus::bill_payment_status, :createdBy, :updatedBy)
                    RETURNING *
                    """)
                    .param("id", id)
                    .param("restaurantId", bill.getRestaurantId())
                    .param("orderId", bill.getOrderId())
                    .param("label", bill.getLabel())
                    .param("splitMode", bill.getSplitMode().name())
                    .param("subtotal", bill.getSubtotalGrossSnapshot())
                    .param("tip", bill.getTipTotalSnapshot())
                    .param("paid", bill.getTotalPaidSnapshot())
                    .param("paymentStatus", bill.getPaymentStatus().name())
                    .param("createdBy", bill.getCreatedBy())
                    .param("updatedBy", bill.getUpdatedBy())
                    .query(this::mapBill)
                    .single();
            List<OrderBillAllocationDomain> allocations = new ArrayList<>();
            for (OrderBillAllocationDomain allocation : bill.getAllocations()) {
                UUID allocationId = UUID.randomUUID();
                allocations.add(jdbc.sql("""
                        INSERT INTO order_bill_allocations
                            (id, order_bill_id, order_item_id, quantity, amount)
                        VALUES (:id, :billId, :orderItemId, :quantity, :amount)
                        RETURNING *
                        """)
                        .param("id", allocationId)
                        .param("billId", id)
                        .param("orderItemId", allocation.getOrderItemId())
                        .param("quantity", allocation.getQuantity())
                        .param("amount", allocation.getAmount())
                        .query(this::mapBillAllocation)
                        .single());
            }
            persisted.setAllocations(allocations);
            saved.add(persisted);
        }
        return saved;
    }

    @Override
    public PageResponse<OrderBillDomain> findBills(BillSearchCriteria criteria) {
        Map<String, Object> params = new HashMap<>();
        params.put("restaurantId", criteria.restaurantId());
        params.put("orderId", criteria.orderId());
        StringBuilder where = new StringBuilder("""
                FROM order_bills b
                WHERE b.restaurant_id = :restaurantId AND b.order_id = :orderId
                """);
        if (criteria.paymentStatus() != null) {
            where.append(" AND b.payment_status = :paymentStatus::bill_payment_status ");
            params.put("paymentStatus", criteria.paymentStatus().name());
        }
        if (criteria.search() != null && !criteria.search().isBlank()) {
            where.append(" AND LOWER(b.label) LIKE :search ");
            params.put("search", "%" + criteria.search().toLowerCase() + "%");
        }
        Long total = jdbc.sql("SELECT COUNT(*) " + where)
                .params(params)
                .query(Long.class)
                .single();
        List<OrderBillDomain> content = jdbc.sql("SELECT b.* " + where
                + " ORDER BY " + resolveSort(criteria.sort(), BILL_SORTS, "b.created_at ASC")
                + " LIMIT :size OFFSET :offset")
                .params(withPage(params, criteria.page(), criteria.size()))
                .query(this::mapBill)
                .list();
        content.forEach(bill -> bill.setAllocations(findAllocations(bill.getId())));
        return PageResponse.of(content, total == null ? 0 : total, criteria.page(), criteria.size());
    }

    @Override
    public List<OrderBillDomain> findBillsByOrder(UUID restaurantId, UUID orderId) {
        List<OrderBillDomain> bills = jdbc.sql("""
                SELECT * FROM order_bills
                 WHERE restaurant_id = :restaurantId AND order_id = :orderId
                 ORDER BY created_at ASC
                """)
                .param("restaurantId", restaurantId)
                .param("orderId", orderId)
                .query(this::mapBill)
                .list();
        bills.forEach(bill -> bill.setAllocations(findAllocations(bill.getId())));
        return bills;
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

    private Map<String, Object> orderParams(OrderDomain order) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", order.getId());
        params.put("restaurantId", order.getRestaurantId());
        params.put("businessDayId", order.getBusinessDayId());
        params.put("originCashSessionId", order.getOriginCashSessionId() != null
                ? order.getOriginCashSessionId()
                : order.getCashSessionId());
        params.put("originDeviceId", order.getOriginDeviceId());
        params.put("businessDate", order.getBusinessDate());
        params.put("dailySequence", order.getDailySequence());
        params.put("publicCode", order.getPublicCode());
        params.put("orderStatus", order.getOrderStatus().name());
        params.put("kitchenStatus", order.getKitchenStatus().name());
        params.put("paymentStatus", order.getPaymentStatus().name());
        params.put("serviceType", order.getServiceType().name());
        params.put("operationMode", order.getOperationModeSnapshot().name());
        params.put("tableId", order.getTableId());
        params.put("customerName", order.getCustomerName());
        params.put("customerPhone", order.getCustomerPhone());
        params.put("deliveryContactName", order.getDeliveryContactName());
        params.put("deliveryPhone", order.getDeliveryPhone());
        params.put("deliveryAddress", order.getDeliveryAddress());
        params.put("deliveryNotes", order.getDeliveryNotes());
        params.put("deliveryFee", value(order.getDeliveryFee()));
        params.put("deliveryStatus", order.getDeliveryStatus().name());
        params.put("prepaymentRequired", order.isPrepaymentRequiredSnapshot());
        params.put("taxRate", value(order.getTaxRateSnapshot()));
        params.put("subtotalGross", value(order.getSubtotalGrossSnapshot()));
        params.put("taxAmount", value(order.getTaxAmountSnapshot()));
        params.put("totalGross", value(order.getTotalGrossSnapshot()));
        params.put("tipTotal", value(order.getTipTotalSnapshot()));
        params.put("refundDue", value(order.getRefundDueSnapshot()));
        params.put("refundedTotal", value(order.getRefundedTotalSnapshot()));
        params.put("paymentExpiresAt", order.getPaymentExpiresAt());
        params.put("paymentExpiredAt", order.getPaymentExpiredAt());
        params.put("expirationProcessedAt", order.getExpirationProcessedAt());
        params.put("notes", order.getNotes());
        params.put("cancelReason", order.getCancelReason());
        params.put("createdBy", order.getCreatedBy());
        params.put("updatedBy", order.getUpdatedBy());
        return params;
    }

    private void appendOrderFilters(StringBuilder where, Map<String, Object> params, OrderSearchCriteria c) {
        if (c.orderStatus() != null) {
            where.append(" AND o.order_status = :orderStatus::order_status ");
            params.put("orderStatus", c.orderStatus().name());
        }
        if (c.paymentStatus() != null) {
            where.append(" AND o.payment_status = :paymentStatus::payment_status ");
            params.put("paymentStatus", c.paymentStatus().name());
        }
        if (c.kitchenStatus() != null) {
            where.append(" AND o.kitchen_status = :kitchenStatus::kitchen_status ");
            params.put("kitchenStatus", c.kitchenStatus().name());
        }
        if (c.serviceType() != null) {
            where.append(" AND o.service_type = :serviceType::service_type ");
            params.put("serviceType", c.serviceType().name());
        }
        if (c.tableId() != null) {
            where.append(" AND o.table_id = :tableId ");
            params.put("tableId", c.tableId());
        }
        if (c.dateFrom() != null) {
            where.append(" AND o.created_at >= :dateFrom ");
            params.put("dateFrom", c.dateFrom());
        }
        if (c.dateTo() != null) {
            where.append(" AND o.created_at <= :dateTo ");
            params.put("dateTo", c.dateTo());
        }
        if (c.cashSessionId() != null) {
            where.append(" AND (o.origin_cash_session_id = :cashSessionId OR EXISTS (SELECT 1 FROM payments p WHERE p.order_id = o.id AND p.cash_session_id = :cashSessionId)) ");
            params.put("cashSessionId", c.cashSessionId());
        }
        if (c.cashRegisterId() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM payments p JOIN cash_sessions cs ON cs.id = p.cash_session_id WHERE p.order_id = o.id AND cs.cash_register_id = :cashRegisterId) ");
            params.put("cashRegisterId", c.cashRegisterId());
        }
        if (c.createdBy() != null) {
            where.append(" AND o.created_by = :createdBy ");
            params.put("createdBy", c.createdBy());
        }
        if (c.customer() != null && !c.customer().isBlank()) {
            where.append(" AND (LOWER(o.customer_name) LIKE :customer OR LOWER(o.customer_phone) LIKE :customer) ");
            params.put("customer", "%" + c.customer().toLowerCase() + "%");
        }
        if (c.paymentMethodId() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM payments p WHERE p.order_id = o.id AND p.payment_method_id = :paymentMethodId) ");
            params.put("paymentMethodId", c.paymentMethodId());
        }
        if (c.minTotal() != null) {
            where.append(" AND o.total_gross_snapshot >= :minTotal ");
            params.put("minTotal", c.minTotal());
        }
        if (c.maxTotal() != null) {
            where.append(" AND o.total_gross_snapshot <= :maxTotal ");
            params.put("maxTotal", c.maxTotal());
        }
        if (c.deliveryStatus() != null) {
            where.append(" AND o.delivery_status = :deliveryStatus::delivery_status ");
            params.put("deliveryStatus", c.deliveryStatus().name());
        }
        if (c.paymentPendingState() != null) {
            where.append(c.paymentPendingState() == PaymentPendingState.ACTIVE
                    ? " AND o.order_status = 'AWAITING_PAYMENT' AND o.payment_expired_at IS NULL "
                    : " AND o.order_status = 'AWAITING_PAYMENT' AND o.payment_expired_at IS NOT NULL ");
        }
        if (c.search() != null && !c.search().isBlank()) {
            where.append("""
                    AND (
                        LOWER(o.customer_name) LIKE :search
                        OR LOWER(o.notes) LIKE :search
                        OR LOWER(o.delivery_address) LIKE :search
                        OR LOWER(o.public_code) LIKE :search
                        OR CAST(o.daily_sequence AS TEXT) LIKE :search
                        OR LOWER(TO_CHAR(o.business_date, 'DDMMYY') || '-' ||
                            CASE WHEN o.daily_sequence > 9999 THEN o.daily_sequence::TEXT ELSE LPAD(o.daily_sequence::TEXT, 4, '0') END
                        ) LIKE :search
                        OR LOWER(TO_CHAR(o.business_date, 'DDMMYY') || '-' ||
                            CASE WHEN o.daily_sequence > 9999 THEN o.daily_sequence::TEXT ELSE LPAD(o.daily_sequence::TEXT, 4, '0') END ||
                            '/' || o.public_code
                        ) LIKE :search
                    )
                    """);
            params.put("search", "%" + c.search().toLowerCase() + "%");
        }
    }

    private PageResponse<OrderDomain> queryOrders(StringBuilder where, Map<String, Object> params,
            String orderClause, int page, int size) {
        Long total = jdbc.sql("SELECT COUNT(*)" + where)
                .params(params)
                .query(Long.class)
                .single();
        List<OrderDomain> content = jdbc.sql("""
                SELECT o.*,
                       COALESCE((SELECT SUM(p.amount) FROM payments p
                                  WHERE p.order_id = o.id AND p.status = 'RECORDED'), 0) AS paid_total
                """ + where
                + " ORDER BY " + orderClause
                + " LIMIT :size OFFSET :offset")
                .params(withPage(params, page, size))
                .query(this::mapOrder)
                .list();
        return PageResponse.of(content, total == null ? 0 : total, page, size);
    }

    private String resolveSort(String requested, Map<String, String> whitelist, String fallback) {
        if (requested == null || requested.isBlank()) {
            return fallback;
        }
        String[] parts = requested.split(",", 2);
        String column = whitelist.get(parts[0].trim());
        if (column == null) {
            return fallback;
        }
        String dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()) ? "ASC" : "DESC";
        return column + " " + dir;
    }

    private Map<String, Object> withPage(Map<String, Object> source, int page, int size) {
        Map<String, Object> params = new HashMap<>(source);
        params.put("size", size);
        params.put("offset", (long) page * size);
        return params;
    }

    private List<OrderItemCancellationInventoryDomain> releaseReservationQuantities(
            UUID cancellationId,
            OrderItemCancellationDomain cancellation,
            UUID userId) {
        List<CancellationSource> sources = jdbc.sql("""
                SELECT r.id AS reservation_id,
                       r.ingredient_stock_id,
                       r.master_ingredient_id,
                       r.quantity_base AS remaining_quantity,
                       r.unit_cost_snapshot,
                       req.quantity_base_per_sale_unit
                  FROM inventory_reservations r
                  JOIN order_item_ingredient_requirements req
                    ON req.order_item_id = r.order_item_id
                   AND req.master_ingredient_id = r.master_ingredient_id
                 WHERE r.order_item_id = :orderItemId
                   AND r.status = 'ACTIVE'
                 ORDER BY r.master_ingredient_id
                """)
                .param("orderItemId", cancellation.getOrderItemId())
                .query((rs, rowNum) -> new CancellationSource(
                        rs.getObject("reservation_id", UUID.class),
                        rs.getObject("ingredient_stock_id", UUID.class),
                        rs.getObject("master_ingredient_id", UUID.class),
                        rs.getBigDecimal("remaining_quantity"),
                        rs.getBigDecimal("quantity_base_per_sale_unit"),
                        rs.getBigDecimal("unit_cost_snapshot")))
                .list();

        List<OrderItemCancellationInventoryDomain> entries = new ArrayList<>();
        for (CancellationSource source : sources) {
            BigDecimal requested = source.quantityPerUnit()
                    .multiply(cancellation.getQuantity())
                    .setScale(6, java.math.RoundingMode.HALF_UP);
            BigDecimal released = requested.min(source.remainingQuantity());
            BigDecimal remaining = source.remainingQuantity().subtract(released);
            jdbc.sql("""
                    UPDATE inventory_reservations
                       SET quantity_base = CASE WHEN :remaining > 0 THEN :remaining ELSE quantity_base END,
                           status = CASE WHEN :remaining > 0 THEN 'ACTIVE'::inventory_reservation_status
                                         ELSE 'RELEASED'::inventory_reservation_status END,
                           updated_at = NOW(),
                           updated_by = :userId
                     WHERE id = :reservationId
                    """)
                    .param("remaining", remaining)
                    .param("userId", userId)
                    .param("reservationId", source.sourceId())
                    .update();
            entries.add(saveCancellationInventory(
                    cancellationId,
                    source,
                    released,
                    OrderItemInventoryDisposition.RELEASE_RESERVED,
                    null));
        }
        return entries;
    }

    private List<OrderItemCancellationInventoryDomain> recordConsumedCancellation(
            UUID cancellationId,
            OrderItemCancellationDomain cancellation,
            UUID userId) {
        List<CancellationSource> sources = jdbc.sql("""
                SELECT consumption.id AS consumption_id,
                       consumption.ingredient_stock_id,
                       consumption.master_ingredient_id,
                       consumption.quantity_base AS consumed_quantity,
                       req.quantity_base_per_sale_unit,
                       consumption.unit_cost_snapshot
                  FROM order_item_ingredient_requirements req
                  JOIN LATERAL (
                      SELECT c.*
                        FROM order_item_consumptions c
                       WHERE c.order_item_id = req.order_item_id
                         AND c.master_ingredient_id = req.master_ingredient_id
                       ORDER BY c.created_at DESC
                       LIMIT 1
                  ) consumption ON TRUE
                 WHERE req.order_item_id = :orderItemId
                 ORDER BY consumption.master_ingredient_id
                """)
                .param("orderItemId", cancellation.getOrderItemId())
                .query((rs, rowNum) -> new CancellationSource(
                        rs.getObject("consumption_id", UUID.class),
                        rs.getObject("ingredient_stock_id", UUID.class),
                        rs.getObject("master_ingredient_id", UUID.class),
                        rs.getBigDecimal("consumed_quantity"),
                        rs.getBigDecimal("quantity_base_per_sale_unit"),
                        rs.getBigDecimal("unit_cost_snapshot")))
                .list();

        List<OrderItemCancellationInventoryDomain> entries = new ArrayList<>();
        for (CancellationSource source : sources) {
            BigDecimal affected = source.quantityPerUnit()
                    .multiply(cancellation.getQuantity())
                    .setScale(6, java.math.RoundingMode.HALF_UP);
            UUID transactionId = null;
            if (cancellation.getInventoryDisposition() == OrderItemInventoryDisposition.RESTOCK) {
                StockLock stock = lockStockById(source.stockId());
                BigDecimal resulting = stock.currentStock().add(affected);
                jdbc.sql("""
                        UPDATE ingredient_stocks
                           SET current_stock = :resulting,
                               updated_at = NOW(),
                               updated_by = :userId
                         WHERE id = :stockId
                        """)
                        .param("resulting", resulting)
                        .param("userId", userId)
                        .param("stockId", stock.stockId())
                        .update();
                transactionId = UUID.randomUUID();
                jdbc.sql("""
                        INSERT INTO inventory_transactions
                            (id, ingredient_stock_id, delta, reason, invoice_id, order_id, order_item_id,
                             previous_stock, resulting_stock, notes, created_by)
                        VALUES
                            (:id, :stockId, :delta, 'SALE_REVERSAL', NULL, :orderId, :orderItemId,
                             :previous, :resulting, :notes, :createdBy)
                        """)
                        .param("id", transactionId)
                        .param("stockId", stock.stockId())
                        .param("delta", affected)
                        .param("orderId", cancellation.getOrderId())
                        .param("orderItemId", cancellation.getOrderItemId())
                        .param("previous", stock.currentStock())
                        .param("resulting", resulting)
                        .param("notes", "Restock from order item cancellation " + cancellationId)
                        .param("createdBy", userId)
                        .update();
            }
            entries.add(saveCancellationInventory(
                    cancellationId,
                    source,
                    affected,
                    cancellation.getInventoryDisposition(),
                    transactionId));
        }
        return entries;
    }

    private OrderItemCancellationInventoryDomain saveCancellationInventory(
            UUID cancellationId,
            CancellationSource source,
            BigDecimal quantity,
            OrderItemInventoryDisposition disposition,
            UUID transactionId) {
        UUID id = UUID.randomUUID();
        return jdbc.sql("""
                INSERT INTO order_item_cancellation_inventory
                    (id, cancellation_id, ingredient_stock_id, master_ingredient_id,
                     quantity_base, unit_cost_snapshot, inventory_disposition, inventory_transaction_id)
                VALUES
                    (:id, :cancellationId, :stockId, :ingredientId,
                     :quantity, :unitCost, :disposition::order_item_inventory_disposition, :transactionId)
                RETURNING *
                """)
                .param("id", id)
                .param("cancellationId", cancellationId)
                .param("stockId", source.stockId())
                .param("ingredientId", source.masterIngredientId())
                .param("quantity", quantity)
                .param("unitCost", source.unitCost())
                .param("disposition", disposition.name())
                .param("transactionId", transactionId)
                .query(this::mapCancellationInventory)
                .single();
    }

    private void cancelKitchenTicketLines(UUID orderItemId, BigDecimal quantity) {
        List<KitchenLineBalance> lines = jdbc.sql("""
                SELECT id, quantity - canceled_quantity AS active_quantity
                  FROM kitchen_ticket_lines
                 WHERE order_item_id = :orderItemId
                   AND quantity > canceled_quantity
                 ORDER BY id
                """)
                .param("orderItemId", orderItemId)
                .query((rs, rowNum) -> new KitchenLineBalance(
                        rs.getObject("id", UUID.class),
                        rs.getBigDecimal("active_quantity")))
                .list();
        BigDecimal remaining = quantity;
        for (KitchenLineBalance line : lines) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal canceled = remaining.min(line.activeQuantity());
            jdbc.sql("""
                    UPDATE kitchen_ticket_lines
                       SET canceled_quantity = canceled_quantity + :quantity
                     WHERE id = :lineId
                    """)
                    .param("quantity", canceled)
                    .param("lineId", line.id())
                    .update();
            remaining = remaining.subtract(canceled);
        }
    }

    private BigDecimal sumPaymentColumn(UUID orderId, String column) {
        BigDecimal value = jdbc.sql("SELECT COALESCE(SUM(" + column + "), 0) FROM payments WHERE order_id = :orderId AND status = 'RECORDED'")
                .param("orderId", orderId)
                .query(BigDecimal.class)
                .single();
        return value == null ? BigDecimal.ZERO : value;
    }

    private StockLock lockStock(UUID restaurantId, UUID masterIngredientId) {
        return jdbc.sql("""
                SELECT id, current_stock
                  FROM ingredient_stocks
                 WHERE restaurant_id = :restaurantId
                   AND master_ingredient_id = :ingredientId
                   AND deleted_at IS NULL
                 FOR UPDATE
                """)
                .param("restaurantId", restaurantId)
                .param("ingredientId", masterIngredientId)
                .query((rs, rowNum) -> new StockLock(rs.getObject("id", UUID.class), rs.getBigDecimal("current_stock")))
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Ingredient is not active in this restaurant."));
    }

    private StockLock lockStockById(UUID stockId) {
        return jdbc.sql("""
                SELECT id, current_stock
                  FROM ingredient_stocks
                 WHERE id = :stockId
                 FOR UPDATE
                """)
                .param("stockId", stockId)
                .query((rs, rowNum) -> new StockLock(rs.getObject("id", UUID.class), rs.getBigDecimal("current_stock")))
                .single();
    }

    private BigDecimal activeReservedQuantity(UUID stockId) {
        BigDecimal value = jdbc.sql("""
                SELECT COALESCE(SUM(quantity_base), 0)
                  FROM inventory_reservations
                 WHERE ingredient_stock_id = :stockId
                   AND status = 'ACTIVE'
                """)
                .param("stockId", stockId)
                .query(BigDecimal.class)
                .single();
        return value == null ? BigDecimal.ZERO : value;
    }

    private void hydrateTemplateSnapshots(List<OrderItemDomain> items) {
        for (OrderItemDomain item : items) {
            List<OrderItemTemplateSlotDomain> slots = jdbc.sql("""
                    SELECT * FROM order_item_template_slots
                     WHERE order_item_id = :orderItemId
                     ORDER BY sort_order ASC
                    """)
                    .param("orderItemId", item.getId())
                    .query(this::mapTemplateSlotSnapshot)
                    .list();
            slots.forEach(slot -> slot.setOptions(jdbc.sql("""
                    SELECT * FROM order_item_template_options
                     WHERE order_item_template_slot_id = :slotId
                     ORDER BY created_at ASC
                    """)
                    .param("slotId", slot.getId())
                    .query(this::mapTemplateOptionSnapshot)
                    .list()));
            item.setTemplateSlots(slots);
        }
    }

    private void hydrateTaxes(UUID orderId, OrderDomain domain, List<OrderItemDomain> items) {
        domain.setTaxes(jdbc.sql("SELECT * FROM order_taxes WHERE order_id = :orderId ORDER BY created_at ASC")
                .param("orderId", orderId)
                .query(this::mapOrderTax)
                .list());
        List<OrderItemTaxDomain> itemTaxes = jdbc.sql("""
                SELECT * FROM order_item_taxes
                 WHERE order_item_id IN (SELECT id FROM order_items WHERE order_id = :orderId)
                 ORDER BY created_at ASC
                """)
                .param("orderId", orderId)
                .query(this::mapOrderItemTax)
                .list();
        Map<UUID, List<OrderItemTaxDomain>> taxesByItem = itemTaxes.stream()
                .collect(Collectors.groupingBy(OrderItemTaxDomain::getOrderItemId));
        items.forEach(item -> item.setTaxes(taxesByItem.getOrDefault(item.getId(), List.of())));
    }

    private List<KitchenTicketDomain> findTicketsByOrder(UUID orderId) {
        List<KitchenTicketDomain> tickets = jdbc.sql("""
                SELECT kt.*,
                       o.business_date AS order_business_date,
                       o.daily_sequence AS order_daily_sequence,
                       o.public_code AS order_public_code,
                       o.customer_name AS order_customer_name
                  FROM kitchen_tickets kt
                  JOIN orders o ON o.id = kt.order_id
                 WHERE kt.order_id = :orderId
                 ORDER BY kt.sent_at ASC
                """)
                .param("orderId", orderId)
                .query(this::mapKitchenTicket)
                .list();
        tickets.forEach(ticket -> ticket.setLines(findTicketLines(ticket.getId())));
        return tickets;
    }

    private List<KitchenTicketLineDomain> findTicketLines(UUID ticketId) {
        List<KitchenTicketLineDomain> lines = jdbc.sql("""
                SELECT l.*, oi.line_type
                  FROM kitchen_ticket_lines l
                  JOIN order_items oi ON oi.id = l.order_item_id
                 WHERE l.kitchen_ticket_id = :ticketId
                 ORDER BY l.id ASC
                """)
                .param("ticketId", ticketId)
                .query(this::mapKitchenTicketLine)
                .list();
        hydrateTicketLineTemplateSnapshots(lines);
        return lines;
    }

    private void hydrateTicketLineTemplateSnapshots(List<KitchenTicketLineDomain> lines) {
        for (KitchenTicketLineDomain line : lines) {
            List<OrderItemTemplateSlotDomain> slots = jdbc.sql("""
                    SELECT * FROM order_item_template_slots
                     WHERE order_item_id = :orderItemId
                     ORDER BY sort_order ASC
                    """)
                    .param("orderItemId", line.getOrderItemId())
                    .query(this::mapTemplateSlotSnapshot)
                    .list();
            slots.forEach(slot -> slot.setOptions(jdbc.sql("""
                    SELECT * FROM order_item_template_options
                     WHERE order_item_template_slot_id = :slotId
                     ORDER BY created_at ASC
                    """)
                    .param("slotId", slot.getId())
                    .query(this::mapTemplateOptionSnapshot)
                    .list()));
            line.setTemplateSlots(slots);
        }
    }

    private List<PaymentDomain> findPaymentsByOrder(UUID orderId) {
        return jdbc.sql("""
                SELECT * FROM payments WHERE order_id = :orderId ORDER BY created_at ASC
                """)
                .param("orderId", orderId)
                .query(this::mapPayment)
                .list();
    }

    private List<OrderItemCancellationDomain> findItemCancellations(UUID orderItemId) {
        List<OrderItemCancellationDomain> cancellations = jdbc.sql("""
                SELECT *
                  FROM order_item_cancellations
                 WHERE order_item_id = :orderItemId
                 ORDER BY created_at ASC
                """)
                .param("orderItemId", orderItemId)
                .query(this::mapItemCancellation)
                .list();
        cancellations.forEach(cancellation -> cancellation.setInventoryEntries(jdbc.sql("""
                SELECT *
                  FROM order_item_cancellation_inventory
                 WHERE cancellation_id = :cancellationId
                 ORDER BY created_at ASC
                """)
                .param("cancellationId", cancellation.getId())
                .query(this::mapCancellationInventory)
                .list()));
        return cancellations;
    }

    private List<OrderBillAllocationDomain> findAllocations(UUID billId) {
        return jdbc.sql("""
                SELECT * FROM order_bill_allocations WHERE order_bill_id = :billId ORDER BY created_at ASC
                """)
                .param("billId", billId)
                .query(this::mapBillAllocation)
                .list();
    }

    private List<PosTemplateSlotDomain> findPosTemplateSlots(UUID templateId) {
        List<PosTemplateSlotDomain> slots = jdbc.sql("""
                SELECT * FROM template_slots WHERE template_id = :templateId ORDER BY sort_order ASC
                """)
                .param("templateId", templateId)
                .query((rs, rowNum) -> PosTemplateSlotDomain.builder()
                        .slotId(rs.getObject("id", UUID.class))
                        .name(rs.getString("name"))
                        .minSelection(rs.getInt("min_selection"))
                        .maxSelection(rs.getInt("max_selection"))
                        .sortOrder(rs.getInt("sort_order"))
                        .build())
                .list();
        slots.forEach(slot -> slot.setOptions(jdbc.sql("""
                SELECT o.id AS slot_option_id, i.id AS item_id, i.name AS item_name,
                       o.surcharge, o.max_quantity, o.is_default, o.sort_order,
                       i.is_active AS item_active,
                       COALESCE(availability.status::text = 'UNAVAILABLE', false) AS manually_unavailable
                  FROM slot_options o
                  JOIN items i ON i.id = o.item_id
             LEFT JOIN catalog_availability_overrides availability
                    ON availability.restaurant_id = o.restaurant_id
                   AND availability.reference_type = 'PRODUCT'
                   AND availability.reference_id = i.id
                   AND (availability.expires_at IS NULL OR availability.expires_at > NOW())
                 WHERE o.slot_id = :slotId
                 ORDER BY o.sort_order ASC
                """)
                .param("slotId", slot.getSlotId())
                .query((rs, rowNum) -> PosTemplateOptionDomain.builder()
                        .slotOptionId(rs.getObject("slot_option_id", UUID.class))
                        .itemId(rs.getObject("item_id", UUID.class))
                        .itemName(rs.getString("item_name"))
                        .surcharge(rs.getBigDecimal("surcharge"))
                        .maxQuantity(rs.getInt("max_quantity"))
                        .isDefault(rs.getBoolean("is_default"))
                        .available(rs.getBoolean("item_active") && !rs.getBoolean("manually_unavailable"))
                        .lowStock(false)
                        .unavailableReason(!rs.getBoolean("item_active")
                                ? "Producto inactivo"
                                : rs.getBoolean("manually_unavailable") ? "Agotado manualmente" : null)
                        .sortOrder(rs.getInt("sort_order"))
                        .build())
                .list()));
        return slots;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private OrderDomain mapOrder(ResultSet rs, int rowNum) throws SQLException {
        UUID originSession = rs.getObject("origin_cash_session_id", UUID.class);
        return OrderDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .businessDayId(rs.getObject("business_day_id", UUID.class))
                .cashSessionId(originSession)
                .originCashSessionId(originSession)
                .originDeviceId(rs.getObject("origin_device_id", UUID.class))
                .businessDate(rs.getObject("business_date", LocalDate.class))
                .dailySequence((Integer) rs.getObject("daily_sequence"))
                .publicCode(rs.getString("public_code"))
                .orderStatus(OrderStatus.valueOf(rs.getString("order_status")))
                .kitchenStatus(KitchenStatus.valueOf(rs.getString("kitchen_status")))
                .paymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")))
                .serviceType(ServiceType.valueOf(rs.getString("service_type")))
                .operationModeSnapshot(OperationMode.valueOf(rs.getString("operation_mode_snapshot")))
                .tableId(rs.getObject("table_id", UUID.class))
                .customerName(rs.getString("customer_name"))
                .customerPhone(rs.getString("customer_phone"))
                .deliveryContactName(rs.getString("delivery_contact_name"))
                .deliveryPhone(rs.getString("delivery_phone"))
                .deliveryAddress(rs.getString("delivery_address"))
                .deliveryNotes(rs.getString("delivery_notes"))
                .deliveryFee(rs.getBigDecimal("delivery_fee"))
                .deliveryStatus(DeliveryStatus.valueOf(rs.getString("delivery_status")))
                .prepaymentRequiredSnapshot(rs.getBoolean("prepayment_required_snapshot"))
                .taxRateSnapshot(rs.getBigDecimal("tax_rate_snapshot"))
                .subtotalGrossSnapshot(rs.getBigDecimal("subtotal_gross_snapshot"))
                .taxAmountSnapshot(rs.getBigDecimal("tax_amount_snapshot"))
                .totalGrossSnapshot(rs.getBigDecimal("total_gross_snapshot"))
                .tipTotalSnapshot(rs.getBigDecimal("tip_total_snapshot"))
                .refundDueSnapshot(rs.getBigDecimal("refund_due_snapshot"))
                .refundedTotalSnapshot(rs.getBigDecimal("refunded_total_snapshot"))
                .paidTotal(hasColumn(rs, "paid_total") ? rs.getBigDecimal("paid_total") : BigDecimal.ZERO)
                .remainingBalance(BigDecimal.ZERO)
                .paymentExpiresAt(rs.getObject("payment_expires_at", OffsetDateTime.class))
                .paymentExpiredAt(rs.getObject("payment_expired_at", OffsetDateTime.class))
                .expirationProcessedAt(rs.getObject("expiration_processed_at", OffsetDateTime.class))
                .notes(rs.getString("notes"))
                .completedAt(rs.getObject("completed_at", OffsetDateTime.class))
                .canceledAt(rs.getObject("canceled_at", OffsetDateTime.class))
                .cancelReason(rs.getString("cancel_reason"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .items(new ArrayList<>())
                .taxes(new ArrayList<>())
                .kitchenTickets(new ArrayList<>())
                .payments(new ArrayList<>())
                .refunds(new ArrayList<>())
                .build();
    }

    private OrderItemDomain mapOrderItem(ResultSet rs, int rowNum) throws SQLException {
        return OrderItemDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .orderId(rs.getObject("order_id", UUID.class))
                .lineType(OrderLineType.valueOf(rs.getString("line_type")))
                .itemId(rs.getObject("item_id", UUID.class))
                .templateId(rs.getObject("template_id", UUID.class))
                .submenuNodeId(rs.getObject("submenu_node_id", UUID.class))
                .itemNameSnapshot(rs.getString("item_name_snapshot"))
                .unitPriceSnapshot(rs.getBigDecimal("unit_price_snapshot"))
                .theoreticalCostSnapshot(rs.getBigDecimal("theoretical_cost_snapshot"))
                .quantity(rs.getBigDecimal("quantity"))
                .canceledQuantity(rs.getBigDecimal("canceled_quantity"))
                .subtotalGrossSnapshot(rs.getBigDecimal("subtotal_gross_snapshot"))
                .notes(rs.getString("notes"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .taxes(new ArrayList<>())
                .templateSlots(new ArrayList<>())
                .cancellations(new ArrayList<>())
                .build();
    }

    private OrderItemTemplateSlotDomain mapTemplateSlotSnapshot(ResultSet rs, int rowNum) throws SQLException {
        return OrderItemTemplateSlotDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .orderItemId(rs.getObject("order_item_id", UUID.class))
                .templateSlotId(rs.getObject("template_slot_id", UUID.class))
                .slotNameSnapshot(rs.getString("slot_name_snapshot"))
                .minSelectionSnapshot(rs.getInt("min_selection_snapshot"))
                .maxSelectionSnapshot(rs.getInt("max_selection_snapshot"))
                .sortOrder(rs.getInt("sort_order"))
                .options(new ArrayList<>())
                .build();
    }

    private OrderItemTemplateOptionDomain mapTemplateOptionSnapshot(ResultSet rs, int rowNum) throws SQLException {
        return OrderItemTemplateOptionDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .orderItemTemplateSlotId(rs.getObject("order_item_template_slot_id", UUID.class))
                .slotOptionId(rs.getObject("slot_option_id", UUID.class))
                .itemId(rs.getObject("item_id", UUID.class))
                .itemNameSnapshot(rs.getString("item_name_snapshot"))
                .quantity(rs.getBigDecimal("quantity"))
                .surchargeSnapshot(rs.getBigDecimal("surcharge_snapshot"))
                .theoreticalCostSnapshot(rs.getBigDecimal("theoretical_cost_snapshot"))
                .build();
    }

    private OrderItemCancellationDomain mapItemCancellation(ResultSet rs, int rowNum) throws SQLException {
        return OrderItemCancellationDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .orderId(rs.getObject("order_id", UUID.class))
                .orderItemId(rs.getObject("order_item_id", UUID.class))
                .quantity(rs.getBigDecimal("quantity"))
                .grossAmount(rs.getBigDecimal("gross_amount"))
                .reason(rs.getString("reason"))
                .kitchenStatusSnapshot(KitchenStatus.valueOf(rs.getString("kitchen_status_snapshot")))
                .inventoryDisposition(OrderItemInventoryDisposition.valueOf(rs.getString("inventory_disposition")))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .systemGenerated(rs.getBoolean("system_generated"))
                .inventoryEntries(new ArrayList<>())
                .build();
    }

    private OrderItemCancellationInventoryDomain mapCancellationInventory(ResultSet rs, int rowNum)
            throws SQLException {
        return OrderItemCancellationInventoryDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .cancellationId(rs.getObject("cancellation_id", UUID.class))
                .ingredientStockId(rs.getObject("ingredient_stock_id", UUID.class))
                .masterIngredientId(rs.getObject("master_ingredient_id", UUID.class))
                .quantityBase(rs.getBigDecimal("quantity_base"))
                .unitCostSnapshot(rs.getBigDecimal("unit_cost_snapshot"))
                .inventoryDisposition(OrderItemInventoryDisposition.valueOf(rs.getString("inventory_disposition")))
                .inventoryTransactionId(rs.getObject("inventory_transaction_id", UUID.class))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
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

    private PosCatalogEntryDomain mapPosCatalogEntry(ResultSet rs, int rowNum) throws SQLException {
        boolean manuallyUnavailable = rs.getBoolean("manually_unavailable");
        return PosCatalogEntryDomain.builder()
                .nodeId(rs.getObject("node_id", UUID.class))
                .menuId(rs.getObject("menu_id", UUID.class))
                .menuName(rs.getString("menu_name"))
                .submenuId(rs.getObject("submenu_id", UUID.class))
                .submenuName(rs.getString("submenu_name"))
                .referenceType(CatalogReferenceType.valueOf(rs.getString("node_type")))
                .referenceId(rs.getObject("reference_id", UUID.class))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .price(rs.getBigDecimal("price"))
                .available(!manuallyUnavailable)
                .lowStock(false)
                .unavailableReason(manuallyUnavailable ? "Agotado manualmente" : null)
                .sortOrder(rs.getInt("sort_order"))
                .build();
    }

    private KitchenTicketDomain mapKitchenTicket(ResultSet rs, int rowNum) throws SQLException {
        return KitchenTicketDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .orderId(rs.getObject("order_id", UUID.class))
                .orderBusinessDate(getOptionalLocalDate(rs, "order_business_date"))
                .orderDailySequence(getOptionalInteger(rs, "order_daily_sequence"))
                .orderPublicCode(getOptionalString(rs, "order_public_code"))
                .customerName(getOptionalString(rs, "order_customer_name"))
                .status(KitchenTicketStatus.valueOf(rs.getString("status")))
                .sentAt(rs.getObject("sent_at", OffsetDateTime.class))
                .sentBy(rs.getObject("sent_by", UUID.class))
                .startedAt(rs.getObject("started_at", OffsetDateTime.class))
                .startedBy(rs.getObject("started_by", UUID.class))
                .readyAt(rs.getObject("ready_at", OffsetDateTime.class))
                .readyBy(rs.getObject("ready_by", UUID.class))
                .canceledAt(rs.getObject("canceled_at", OffsetDateTime.class))
                .canceledBy(rs.getObject("canceled_by", UUID.class))
                .notes(rs.getString("notes"))
                .lines(new ArrayList<>())
                .build();
    }

    private LocalDate getOptionalLocalDate(ResultSet rs, String column) throws SQLException {
        if (!hasColumn(rs, column)) {
            return null;
        }
        return rs.getObject(column, LocalDate.class);
    }

    private Integer getOptionalInteger(ResultSet rs, String column) throws SQLException {
        if (!hasColumn(rs, column)) {
            return null;
        }
        return (Integer) rs.getObject(column);
    }

    private String getOptionalString(ResultSet rs, String column) throws SQLException {
        if (!hasColumn(rs, column)) {
            return null;
        }
        return rs.getString(column);
    }

    private boolean hasColumn(ResultSet rs, String column) throws SQLException {
        try {
            rs.findColumn(column);
            return true;
        } catch (SQLException exception) {
            return false;
        }
    }

    private KitchenTicketLineDomain mapKitchenTicketLine(ResultSet rs, int rowNum) throws SQLException {
        return KitchenTicketLineDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .kitchenTicketId(rs.getObject("kitchen_ticket_id", UUID.class))
                .orderItemId(rs.getObject("order_item_id", UUID.class))
                .lineType(hasColumn(rs, "line_type") ? OrderLineType.valueOf(rs.getString("line_type")) : null)
                .quantity(rs.getBigDecimal("quantity"))
                .canceledQuantity(rs.getBigDecimal("canceled_quantity"))
                .itemNameSnapshot(rs.getString("item_name_snapshot"))
                .notes(rs.getString("notes"))
                .build();
    }

    private InventoryReservationDomain mapReservation(ResultSet rs, int rowNum) throws SQLException {
        return InventoryReservationDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .orderId(rs.getObject("order_id", UUID.class))
                .orderItemId(rs.getObject("order_item_id", UUID.class))
                .ingredientStockId(rs.getObject("ingredient_stock_id", UUID.class))
                .masterIngredientId(rs.getObject("master_ingredient_id", UUID.class))
                .quantityBase(rs.getBigDecimal("quantity_base"))
                .unitCostSnapshot(rs.getBigDecimal("unit_cost_snapshot"))
                .status(InventoryReservationStatus.valueOf(rs.getString("status")))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .build();
    }

    private OrderItemIngredientRequirementDomain mapIngredientRequirement(ResultSet rs, int rowNum)
            throws SQLException {
        return OrderItemIngredientRequirementDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .orderId(rs.getObject("order_id", UUID.class))
                .orderItemId(rs.getObject("order_item_id", UUID.class))
                .masterIngredientId(rs.getObject("master_ingredient_id", UUID.class))
                .ingredientNameSnapshot(rs.getString("ingredient_name_snapshot"))
                .quantityBasePerSaleUnit(rs.getBigDecimal("quantity_base_per_sale_unit"))
                .unitCostSnapshot(rs.getBigDecimal("unit_cost_snapshot"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .build();
    }

    private PaymentMethodDomain mapPaymentMethod(ResultSet rs, int rowNum) throws SQLException {
        return PaymentMethodDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .code(rs.getString("code"))
                .name(rs.getString("name"))
                .type(PaymentMethodType.valueOf(rs.getString("type")))
                .isActive(rs.getBoolean("is_active"))
                .requiresReference(rs.getBoolean("requires_reference"))
                .sortOrder(rs.getInt("sort_order"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .build();
    }

    private PaymentDomain mapPayment(ResultSet rs, int rowNum) throws SQLException {
        return PaymentDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .orderId(rs.getObject("order_id", UUID.class))
                .orderBillId(rs.getObject("order_bill_id", UUID.class))
                .paymentMethodId(rs.getObject("payment_method_id", UUID.class))
                .cashSessionId(rs.getObject("cash_session_id", UUID.class))
                .deviceId(rs.getObject("device_id", UUID.class))
                .amount(rs.getBigDecimal("amount"))
                .tipAmount(rs.getBigDecimal("tip_amount"))
                .status(PaymentRecordStatus.valueOf(rs.getString("status")))
                .externalReference(rs.getString("external_reference"))
                .notes(rs.getString("notes"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .voidedAt(rs.getObject("voided_at", OffsetDateTime.class))
                .voidedBy(rs.getObject("voided_by", UUID.class))
                .build();
    }

    private PaymentRefundDomain mapRefund(ResultSet rs, int rowNum) throws SQLException {
        return PaymentRefundDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .orderId(rs.getObject("order_id", UUID.class))
                .paymentId(rs.getObject("payment_id", UUID.class))
                .cashSessionId(rs.getObject("cash_session_id", UUID.class))
                .deviceId(rs.getObject("device_id", UUID.class))
                .amount(rs.getBigDecimal("amount"))
                .status(PaymentRefundStatus.valueOf(rs.getString("status")))
                .reason(rs.getString("reason"))
                .externalReference(rs.getString("external_reference"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .voidedAt(rs.getObject("voided_at", OffsetDateTime.class))
                .voidedBy(rs.getObject("voided_by", UUID.class))
                .build();
    }

    private OrderBillDomain mapBill(ResultSet rs, int rowNum) throws SQLException {
        return OrderBillDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .orderId(rs.getObject("order_id", UUID.class))
                .label(rs.getString("label"))
                .splitMode(BillSplitMode.valueOf(rs.getString("split_mode")))
                .subtotalGrossSnapshot(rs.getBigDecimal("subtotal_gross_snapshot"))
                .tipTotalSnapshot(rs.getBigDecimal("tip_total_snapshot"))
                .totalPaidSnapshot(rs.getBigDecimal("total_paid_snapshot"))
                .paymentStatus(BillPaymentStatus.valueOf(rs.getString("payment_status")))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .allocations(new ArrayList<>())
                .build();
    }

    private OrderBillAllocationDomain mapBillAllocation(ResultSet rs, int rowNum) throws SQLException {
        return OrderBillAllocationDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .orderBillId(rs.getObject("order_bill_id", UUID.class))
                .orderItemId(rs.getObject("order_item_id", UUID.class))
                .quantity(rs.getBigDecimal("quantity"))
                .amount(rs.getBigDecimal("amount"))
                .build();
    }

    private record StockLock(UUID stockId, BigDecimal currentStock) {
    }

    private record CancellationSource(
            UUID sourceId,
            UUID stockId,
            UUID masterIngredientId,
            BigDecimal remainingQuantity,
            BigDecimal quantityPerUnit,
            BigDecimal unitCost) {
    }

    private record KitchenLineBalance(UUID id, BigDecimal activeQuantity) {
    }
}
