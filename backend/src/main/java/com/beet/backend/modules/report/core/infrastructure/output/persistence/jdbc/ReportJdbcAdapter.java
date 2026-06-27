package com.beet.backend.modules.report.core.infrastructure.output.persistence.jdbc;

import com.beet.backend.modules.report.cash.domain.model.BusinessDayDetailReport;
import com.beet.backend.modules.report.cash.domain.model.BusinessDayReportRow;
import com.beet.backend.modules.report.catalog.domain.model.CatalogReportRow;
import com.beet.backend.modules.report.core.domain.model.ReportGrouping;
import com.beet.backend.modules.report.core.domain.model.ReportOverview;
import com.beet.backend.modules.report.core.domain.model.ReportPage;
import com.beet.backend.modules.report.core.domain.model.ReportQuery;
import com.beet.backend.modules.report.core.domain.model.ReportSeriesPoint;
import com.beet.backend.modules.report.core.domain.spi.ReportQueryPort;
import com.beet.backend.modules.report.inventory.domain.model.InventoryConsumptionRow;
import com.beet.backend.modules.report.inventory.domain.model.InventoryValuationReport;
import com.beet.backend.modules.report.inventory.domain.model.LowStockReportRow;
import com.beet.backend.modules.report.payments.domain.model.PaymentMethodReportRow;
import com.beet.backend.modules.report.sales.domain.model.SalesSeriesReport;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReportJdbcAdapter implements ReportQueryPort {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final JdbcClient jdbc;

    @Override
    public ReportOverview overview(ReportQuery query) {
        Map<String, Object> params = baseParams(query);
        ReportOverview totals = jdbc.sql("""
                WITH order_totals AS (
                    SELECT
                        COALESCE(SUM(total_gross_snapshot)
                            FILTER (WHERE order_status IN ('OPEN', 'COMPLETED')), 0) gross_sales,
                        COALESCE(SUM(total_gross_snapshot)
                            FILTER (WHERE order_status = 'COMPLETED'), 0) completed_sales,
                        COALESCE(SUM(total_gross_snapshot)
                            FILTER (WHERE order_status = 'OPEN'), 0) open_value,
                        COUNT(*) FILTER (WHERE order_status = 'COMPLETED') completed_orders
                    FROM orders
                    WHERE restaurant_id IN (:restaurantIds)
                      AND business_date BETWEEN :dateFrom AND :dateTo
                      AND order_status <> 'CANCELED'
                ),
                payment_totals AS (
                    SELECT
                        COALESCE(SUM(p.amount), 0) collected,
                        COALESCE(SUM(p.tip_amount), 0) tips
                    FROM payments p
                    JOIN orders o ON o.id = p.order_id
                    WHERE p.restaurant_id IN (:restaurantIds)
                      AND o.business_date BETWEEN :dateFrom AND :dateTo
                      AND p.status = 'RECORDED'
                ),
                refund_totals AS (
                    SELECT COALESCE(SUM(pr.amount), 0) refunds
                    FROM payment_refunds pr
                    JOIN orders o ON o.id = pr.order_id
                    WHERE pr.restaurant_id IN (:restaurantIds)
                      AND o.business_date BETWEEN :dateFrom AND :dateTo
                      AND pr.status = 'RECORDED'
                )
                SELECT ot.*, pt.collected, pt.tips, rt.refunds,
                       EXISTS (
                           SELECT 1 FROM restaurant_business_days bd
                           WHERE bd.restaurant_id IN (:restaurantIds)
                             AND bd.business_date BETWEEN :dateFrom AND :dateTo
                             AND bd.status = 'OPEN'
                       ) provisional
                FROM order_totals ot, payment_totals pt, refund_totals rt
                """)
                .params(params)
                .query((rs, rowNum) -> {
                    BigDecimal completedSales = money(rs.getBigDecimal("completed_sales"));
                    long completedOrders = rs.getLong("completed_orders");
                    BigDecimal collected = money(rs.getBigDecimal("collected"));
                    BigDecimal refunds = money(rs.getBigDecimal("refunds"));
                    return new ReportOverview(
                            query.dateFrom(), query.dateTo(), rs.getBoolean("provisional"),
                            money(rs.getBigDecimal("gross_sales")), completedSales,
                            money(rs.getBigDecimal("open_value")), completedOrders,
                            completedOrders == 0 ? ZERO : completedSales.divide(
                                    BigDecimal.valueOf(completedOrders), 4, java.math.RoundingMode.HALF_UP),
                            collected, money(rs.getBigDecimal("tips")), refunds,
                            collected.subtract(refunds), List.of(), List.of());
                })
                .single();

        List<ReportOverview.ServiceTypeTotal> serviceTypes = jdbc.sql("""
                SELECT service_type::text service_type, COUNT(*) order_count,
                       COALESCE(SUM(total_gross_snapshot), 0) gross_sales
                FROM orders
                WHERE restaurant_id IN (:restaurantIds)
                  AND business_date BETWEEN :dateFrom AND :dateTo
                  AND order_status IN ('OPEN', 'COMPLETED')
                GROUP BY service_type
                ORDER BY gross_sales DESC
                """)
                .params(params)
                .query((rs, rowNum) -> new ReportOverview.ServiceTypeTotal(
                        rs.getString("service_type"), rs.getLong("order_count"),
                        money(rs.getBigDecimal("gross_sales"))))
                .list();

        List<ReportOverview.RestaurantTotal> restaurants = jdbc.sql("""
                SELECT r.id, r.name,
                       COALESCE(SUM(o.total_gross_snapshot)
                           FILTER (WHERE o.order_status IN ('OPEN', 'COMPLETED')), 0) gross_sales,
                       COUNT(*) FILTER (WHERE o.order_status = 'COMPLETED') completed_orders,
                       COALESCE((
                           SELECT SUM(p.amount)
                           FROM payments p
                           JOIN orders paid_order ON paid_order.id = p.order_id
                           WHERE p.restaurant_id = r.id
                             AND paid_order.business_date BETWEEN :dateFrom AND :dateTo
                             AND p.status = 'RECORDED'
                       ), 0) collected
                FROM restaurants r
                LEFT JOIN orders o
                  ON o.restaurant_id = r.id
                 AND o.business_date BETWEEN :dateFrom AND :dateTo
                 AND o.order_status <> 'CANCELED'
                WHERE r.id IN (:restaurantIds)
                GROUP BY r.id, r.name
                ORDER BY gross_sales DESC, r.name
                """)
                .params(params)
                .query((rs, rowNum) -> new ReportOverview.RestaurantTotal(
                        uuid(rs, "id"), rs.getString("name"), money(rs.getBigDecimal("gross_sales")),
                        money(rs.getBigDecimal("collected")), rs.getLong("completed_orders")))
                .list();

        return new ReportOverview(
                totals.dateFrom(), totals.dateTo(), totals.provisional(), totals.grossSales(),
                totals.completedSales(), totals.openOrderValue(), totals.completedOrders(),
                totals.averageTicket(), totals.collected(), totals.tips(), totals.refunds(),
                totals.netCollected(), serviceTypes, restaurants);
    }

    @Override
    public SalesSeriesReport salesSeries(ReportQuery query) {
        Map<String, Object> params = baseParams(query);
        params.put("grouping", grouping(query.grouping()));
        List<ReportSeriesPoint> points = jdbc.sql("""
                WITH order_series AS (
                    SELECT date_trunc(:grouping, business_date::timestamp)::date period,
                           COALESCE(SUM(total_gross_snapshot)
                               FILTER (WHERE order_status IN ('OPEN', 'COMPLETED')), 0) gross_sales,
                           COALESCE(SUM(total_gross_snapshot)
                               FILTER (WHERE order_status = 'COMPLETED'), 0) completed_sales,
                           COUNT(*) FILTER (WHERE order_status = 'COMPLETED') order_count
                    FROM orders
                    WHERE restaurant_id IN (:restaurantIds)
                      AND business_date BETWEEN :dateFrom AND :dateTo
                      AND order_status <> 'CANCELED'
                    GROUP BY 1
                ),
                payment_series AS (
                    SELECT date_trunc(:grouping, o.business_date::timestamp)::date period,
                           COALESCE(SUM(p.amount), 0) collected
                    FROM payments p
                    JOIN orders o ON o.id = p.order_id
                    WHERE p.restaurant_id IN (:restaurantIds)
                      AND o.business_date BETWEEN :dateFrom AND :dateTo
                      AND p.status = 'RECORDED'
                    GROUP BY 1
                ),
                refund_series AS (
                    SELECT date_trunc(:grouping, o.business_date::timestamp)::date period,
                           COALESCE(SUM(pr.amount), 0) refunds
                    FROM payment_refunds pr
                    JOIN orders o ON o.id = pr.order_id
                    WHERE pr.restaurant_id IN (:restaurantIds)
                      AND o.business_date BETWEEN :dateFrom AND :dateTo
                      AND pr.status = 'RECORDED'
                    GROUP BY 1
                )
                SELECT COALESCE(os.period, ps.period, rs.period) period,
                       COALESCE(os.gross_sales, 0) gross_sales,
                       COALESCE(os.completed_sales, 0) completed_sales,
                       COALESCE(ps.collected, 0) collected,
                       COALESCE(rs.refunds, 0) refunds,
                       COALESCE(os.order_count, 0) order_count
                FROM order_series os
                FULL OUTER JOIN payment_series ps ON ps.period = os.period
                FULL OUTER JOIN refund_series rs ON rs.period = COALESCE(os.period, ps.period)
                ORDER BY period
                """)
                .params(params)
                .query((rs, rowNum) -> new ReportSeriesPoint(
                        rs.getObject("period", LocalDate.class),
                        money(rs.getBigDecimal("gross_sales")),
                        money(rs.getBigDecimal("completed_sales")),
                        money(rs.getBigDecimal("collected")),
                        money(rs.getBigDecimal("refunds")),
                        rs.getLong("order_count")))
                .list();
        boolean provisional = hasOpenDay(query);
        return new SalesSeriesReport(provisional, points);
    }

    @Override
    public ReportPage<PaymentMethodReportRow> paymentMethods(ReportQuery query) {
        Map<String, Object> params = pageParams(query);
        String search = searchClause(query.search(), "pm.name", "pm.code", "r.name");
        String from = """
                FROM payment_methods pm
                JOIN restaurants r ON r.id = pm.restaurant_id
                LEFT JOIN (
                    SELECT p.payment_method_id,
                           COUNT(*) payment_count,
                           SUM(p.amount) collected,
                           SUM(p.tip_amount) tips
                    FROM payments p
                    JOIN orders o ON o.id = p.order_id
                    WHERE p.restaurant_id IN (:restaurantIds)
                      AND o.business_date BETWEEN :dateFrom AND :dateTo
                      AND p.status = 'RECORDED'
                    GROUP BY p.payment_method_id
                ) paid ON paid.payment_method_id = pm.id
                LEFT JOIN (
                    SELECT p.payment_method_id, SUM(pr.amount) refunds
                    FROM payment_refunds pr
                    JOIN payments p ON p.id = pr.payment_id
                    JOIN orders o ON o.id = pr.order_id
                    WHERE pr.restaurant_id IN (:restaurantIds)
                      AND o.business_date BETWEEN :dateFrom AND :dateTo
                      AND pr.status = 'RECORDED'
                    GROUP BY p.payment_method_id
                ) refunded ON refunded.payment_method_id = pm.id
                WHERE pm.restaurant_id IN (:restaurantIds)
                """ + search;
        long count = count(from, params);
        String order = switch (query.sort()) {
            case "collected,asc" -> "collected ASC";
            case "name,asc" -> "pm.name ASC";
            case "name,desc" -> "pm.name DESC";
            default -> "collected DESC, pm.name ASC";
        };
        List<PaymentMethodReportRow> rows = jdbc.sql("""
                SELECT pm.restaurant_id, r.name restaurant_name, pm.id, pm.code, pm.name,
                       pm.type::text method_type, COALESCE(paid.payment_count, 0) payment_count,
                       COALESCE(paid.collected, 0) collected, COALESCE(paid.tips, 0) tips,
                       COALESCE(refunded.refunds, 0) refunds,
                       COALESCE(paid.collected, 0) - COALESCE(refunded.refunds, 0) net_collected
                """ + from + " ORDER BY " + order + " LIMIT :limit OFFSET :offset")
                .params(params)
                .query((rs, rowNum) -> new PaymentMethodReportRow(
                        uuid(rs, "restaurant_id"), rs.getString("restaurant_name"), uuid(rs, "id"),
                        rs.getString("code"), rs.getString("name"), rs.getString("method_type"),
                        rs.getLong("payment_count"), money(rs.getBigDecimal("collected")),
                        money(rs.getBigDecimal("tips")), money(rs.getBigDecimal("refunds")),
                        money(rs.getBigDecimal("net_collected"))))
                .list();
        return new ReportPage<>(rows, count, query.page(), query.size());
    }

    @Override
    public ReportPage<BusinessDayReportRow> businessDays(ReportQuery query) {
        Map<String, Object> params = pageParams(query);
        String search = searchClause(query.search(), "r.name");
        String from = """
                FROM restaurant_business_days bd
                JOIN restaurants r ON r.id = bd.restaurant_id
                LEFT JOIN LATERAL (
                    SELECT *
                    FROM business_day_closures c
                    WHERE c.business_day_id = bd.id
                    ORDER BY c.closure_sequence DESC
                    LIMIT 1
                ) c ON TRUE
                LEFT JOIN LATERAL (
                    SELECT
                        COALESCE((SELECT SUM(p.amount)
                            FROM payments p
                            JOIN cash_sessions ps ON ps.id = p.cash_session_id
                            WHERE ps.business_day_id = bd.id AND p.status = 'RECORDED'), 0) payments_total,
                        COALESCE((SELECT SUM(p.tip_amount)
                            FROM payments p
                            JOIN cash_sessions ps ON ps.id = p.cash_session_id
                            WHERE ps.business_day_id = bd.id AND p.status = 'RECORDED'), 0) tips_total,
                        COALESCE((SELECT SUM(pr.amount)
                            FROM payment_refunds pr
                            JOIN cash_sessions rs ON rs.id = pr.cash_session_id
                            WHERE rs.business_day_id = bd.id AND pr.status = 'RECORDED'), 0) refunds_total,
                        COALESCE((SELECT SUM(cs.opening_amount)
                            FROM cash_sessions cs WHERE cs.business_day_id = bd.id), 0)
                        + COALESCE((SELECT SUM(p.amount + p.tip_amount)
                            FROM payments p
                            JOIN payment_methods pm ON pm.id = p.payment_method_id
                            JOIN cash_sessions ps ON ps.id = p.cash_session_id
                            WHERE ps.business_day_id = bd.id
                              AND p.status = 'RECORDED' AND pm.type = 'CASH'), 0)
                        - COALESCE((SELECT SUM(pr.amount)
                            FROM payment_refunds pr
                            JOIN payments p ON p.id = pr.payment_id
                            JOIN payment_methods pm ON pm.id = p.payment_method_id
                            JOIN cash_sessions rs ON rs.id = pr.cash_session_id
                            WHERE rs.business_day_id = bd.id
                              AND pr.status = 'RECORDED' AND pm.type = 'CASH'), 0)
                        + COALESCE((SELECT SUM(CASE WHEN direction = 'IN' THEN amount ELSE -amount END)
                            FROM cash_movements m
                            WHERE m.business_day_id = bd.id AND m.status = 'RECORDED'), 0) expected_cash
                ) live ON TRUE
                WHERE bd.restaurant_id IN (:restaurantIds)
                  AND bd.business_date BETWEEN :dateFrom AND :dateTo
                """ + search;
        long count = count(from, params);
        String order = "bd.business_date ASC".equals(sortBusinessDay(query.sort()))
                ? "bd.business_date ASC" : "bd.business_date DESC";
        List<BusinessDayReportRow> rows = jdbc.sql("""
                SELECT bd.id, bd.restaurant_id, r.name restaurant_name, bd.business_date,
                       bd.time_zone_snapshot, bd.status::text status,
                       COALESCE(c.closure_sequence, 0) closure_sequence,
                       CASE WHEN bd.status = 'OPEN' THEN live.payments_total ELSE c.payments_total END payments_total,
                       CASE WHEN bd.status = 'OPEN' THEN live.tips_total ELSE c.tips_total END tips_total,
                       CASE WHEN bd.status = 'OPEN' THEN live.refunds_total ELSE c.refunds_total END refunds_total,
                       CASE WHEN bd.status = 'OPEN' THEN live.expected_cash ELSE c.expected_cash_total END expected_cash_total,
                       CASE WHEN bd.status = 'OPEN' THEN NULL ELSE c.counted_cash_total END counted_cash_total,
                       CASE WHEN bd.status = 'OPEN' THEN NULL ELSE c.difference_total END difference_total,
                       bd.opened_at, bd.closed_at
                """ + from + " ORDER BY " + order + " LIMIT :limit OFFSET :offset")
                .params(params)
                .query((rs, rowNum) -> new BusinessDayReportRow(
                        uuid(rs, "id"), uuid(rs, "restaurant_id"), rs.getString("restaurant_name"),
                        rs.getObject("business_date", LocalDate.class), rs.getString("time_zone_snapshot"),
                        rs.getString("status"), "OPEN".equals(rs.getString("status")),
                        rs.getInt("closure_sequence"), money(rs.getBigDecimal("payments_total")),
                        money(rs.getBigDecimal("tips_total")), money(rs.getBigDecimal("refunds_total")),
                        rs.getBigDecimal("expected_cash_total"), rs.getBigDecimal("counted_cash_total"),
                        rs.getBigDecimal("difference_total"), rs.getObject("opened_at", java.time.OffsetDateTime.class),
                        rs.getObject("closed_at", java.time.OffsetDateTime.class)))
                .list();
        return new ReportPage<>(rows, count, query.page(), query.size());
    }

    @Override
    public BusinessDayDetailReport businessDayDetail(ReportQuery query, UUID businessDayId) {
        Map<String, Object> params = baseParams(query);
        params.put("businessDayId", businessDayId);
        BusinessDayDetailReport base = jdbc.sql("""
                SELECT bd.*, r.name restaurant_name
                FROM restaurant_business_days bd
                JOIN restaurants r ON r.id = bd.restaurant_id
                WHERE bd.id = :businessDayId AND bd.restaurant_id IN (:restaurantIds)
                """)
                .params(params)
                .query((rs, rowNum) -> new BusinessDayDetailReport(
                        uuid(rs, "id"), uuid(rs, "restaurant_id"), rs.getString("restaurant_name"),
                        rs.getObject("business_date", LocalDate.class), rs.getString("time_zone_snapshot"),
                        rs.getString("status"), "OPEN".equals(rs.getString("status")),
                        List.of(), List.of(), List.of()))
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Business day not found."));
        List<BusinessDayDetailReport.Event> events = jdbc.sql("""
                SELECT event_type::text, reason, occurred_at, occurred_by
                FROM business_day_events
                WHERE business_day_id = :businessDayId
                ORDER BY occurred_at
                """)
                .param("businessDayId", businessDayId)
                .query((rs, rowNum) -> new BusinessDayDetailReport.Event(
                        rs.getString("event_type"), rs.getString("reason"),
                        rs.getObject("occurred_at", java.time.OffsetDateTime.class),
                        uuid(rs, "occurred_by")))
                .list();
        List<BusinessDayDetailReport.Closure> closures = jdbc.sql("""
                SELECT * FROM business_day_closures
                WHERE business_day_id = :businessDayId
                ORDER BY closure_sequence
                """)
                .param("businessDayId", businessDayId)
                .query((rs, rowNum) -> new BusinessDayDetailReport.Closure(
                        uuid(rs, "id"), rs.getInt("closure_sequence"),
                        money(rs.getBigDecimal("payments_total")), money(rs.getBigDecimal("tips_total")),
                        money(rs.getBigDecimal("refunds_total")), money(rs.getBigDecimal("cash_in_total")),
                        money(rs.getBigDecimal("cash_out_total")), money(rs.getBigDecimal("expected_cash_total")),
                        money(rs.getBigDecimal("counted_cash_total")), money(rs.getBigDecimal("difference_total")),
                        rs.getString("notes"), rs.getObject("closed_at", java.time.OffsetDateTime.class)))
                .list();
        List<BusinessDayDetailReport.Session> sessions = jdbc.sql("""
                SELECT cs.id, cr.name register_name, cs.status::text status, cs.opening_amount,
                       c.expected_cash, c.counted_cash, c.difference_amount, c.difference_reason,
                       cs.opened_at, cs.closed_at
                FROM cash_sessions cs
                JOIN cash_registers cr ON cr.id = cs.cash_register_id
                LEFT JOIN cash_session_closures c ON c.cash_session_id = cs.id
                WHERE cs.business_day_id = :businessDayId
                ORDER BY cs.opened_at
                """)
                .param("businessDayId", businessDayId)
                .query((rs, rowNum) -> new BusinessDayDetailReport.Session(
                        uuid(rs, "id"), rs.getString("register_name"), rs.getString("status"),
                        money(rs.getBigDecimal("opening_amount")), rs.getBigDecimal("expected_cash"),
                        rs.getBigDecimal("counted_cash"), rs.getBigDecimal("difference_amount"),
                        rs.getString("difference_reason"),
                        rs.getObject("opened_at", java.time.OffsetDateTime.class),
                        rs.getObject("closed_at", java.time.OffsetDateTime.class)))
                .list();
        return new BusinessDayDetailReport(
                base.id(), base.restaurantId(), base.restaurantName(), base.businessDate(),
                base.timeZone(), base.status(), base.provisional(), events, closures, sessions);
    }

    @Override
    public ReportPage<CatalogReportRow> catalog(ReportQuery query, String lineType) {
        Map<String, Object> params = pageParams(query);
        params.put("lineType", lineType);
        String catalogTable = "PRODUCT".equals(lineType)
                ? "items c" : "templates c";
        String catalogFilter = "PRODUCT".equals(lineType)
                ? "c.class = 'PRODUCT' AND c.deleted_at IS NULL" : "c.deleted_at IS NULL";
        String reference = "PRODUCT".equals(lineType) ? "oi.item_id" : "oi.template_id";
        String optionBreakdown = "PRODUCT".equals(lineType)
                ? "NULL::text"
                : """
                    (
                        SELECT string_agg(option_totals.option_name || ' x ' || option_totals.quantity, ', '
                                          ORDER BY option_totals.option_name)
                        FROM (
                            SELECT option.item_name_snapshot option_name,
                                   SUM(option.quantity * (template_item.quantity - template_item.canceled_quantity))::text quantity
                            FROM order_items template_item
                            JOIN orders template_order ON template_order.id = template_item.order_id
                            JOIN order_item_template_slots selected_slot
                              ON selected_slot.order_item_id = template_item.id
                            JOIN order_item_template_options option
                              ON option.order_item_template_slot_id = selected_slot.id
                            WHERE template_item.template_id = c.id
                              AND template_order.restaurant_id IN (:restaurantIds)
                              AND template_order.business_date BETWEEN :dateFrom AND :dateTo
                              AND template_order.order_status IN ('OPEN', 'COMPLETED')
                            GROUP BY option.item_name_snapshot
                        ) option_totals
                    )
                    """;
        String search = searchClause(query.search(), "c.name", "r.name");
        String from = """
                FROM %s
                JOIN restaurants r ON r.id = c.restaurant_id
                LEFT JOIN (
                    SELECT %s reference_id,
                           SUM(oi.quantity - oi.canceled_quantity) sold_quantity,
                           SUM(oi.canceled_quantity) canceled_quantity,
                           SUM(oi.subtotal_gross_snapshot) gross_sales,
                           SUM(CASE WHEN oi.theoretical_cost_snapshot IS NOT NULL
                               THEN oi.theoretical_cost_snapshot * (oi.quantity - oi.canceled_quantity) END) theoretical_cost,
                           BOOL_AND(oi.theoretical_cost_snapshot IS NOT NULL) cost_complete
                    FROM order_items oi
                    JOIN orders o ON o.id = oi.order_id
                    WHERE o.restaurant_id IN (:restaurantIds)
                      AND o.business_date BETWEEN :dateFrom AND :dateTo
                      AND o.order_status IN ('OPEN', 'COMPLETED')
                      AND oi.line_type = :lineType::order_line_type
                    GROUP BY %s
                ) sales ON sales.reference_id = c.id
                WHERE c.restaurant_id IN (:restaurantIds) AND %s
                """.formatted(catalogTable, reference, reference, catalogFilter) + search;
        long count = count(from, params);
        String order = switch (query.sort()) {
            case "name,asc" -> "c.name ASC";
            case "quantity,asc" -> "sold_quantity ASC NULLS FIRST";
            case "sales,asc" -> "gross_sales ASC NULLS FIRST";
            default -> "gross_sales DESC NULLS LAST, c.name ASC";
        };
        List<CatalogReportRow> rows = jdbc.sql("""
                SELECT c.restaurant_id, r.name restaurant_name, c.id, c.name,
                       COALESCE(sales.sold_quantity, 0) sold_quantity,
                       COALESCE(sales.canceled_quantity, 0) canceled_quantity,
                       COALESCE(sales.gross_sales, 0) gross_sales,
                       sales.theoretical_cost,
                       CASE WHEN sales.theoretical_cost IS NULL THEN NULL
                            ELSE sales.gross_sales - sales.theoretical_cost END theoretical_margin,
                       COALESCE(sales.cost_complete, TRUE) cost_complete,
                       %s option_breakdown
                """.formatted(optionBreakdown) + from + " ORDER BY " + order + " LIMIT :limit OFFSET :offset")
                .params(params)
                .query((rs, rowNum) -> new CatalogReportRow(
                        uuid(rs, "restaurant_id"), rs.getString("restaurant_name"), uuid(rs, "id"),
                        rs.getString("name"), lineType, money(rs.getBigDecimal("sold_quantity")),
                        money(rs.getBigDecimal("canceled_quantity")), money(rs.getBigDecimal("gross_sales")),
                        rs.getBigDecimal("theoretical_cost"), rs.getBigDecimal("theoretical_margin"),
                        rs.getBoolean("cost_complete"), rs.getString("option_breakdown")))
                .list();
        return new ReportPage<>(rows, count, query.page(), query.size());
    }

    @Override
    public ReportPage<InventoryConsumptionRow> inventoryConsumption(ReportQuery query) {
        Map<String, Object> params = pageParams(query);
        String search = searchClause(query.search(), "mi.name", "r.name");
        String from = """
                FROM order_item_consumptions c
                JOIN orders o ON o.id = c.order_id
                JOIN master_ingredients mi ON mi.id = c.master_ingredient_id
                JOIN restaurants r ON r.id = c.restaurant_id
                WHERE c.restaurant_id IN (:restaurantIds)
                  AND o.business_date BETWEEN :dateFrom AND :dateTo
                """ + search + """
                GROUP BY c.restaurant_id, r.name, c.master_ingredient_id, mi.name
                """;
        long count = jdbc.sql("SELECT COUNT(*) FROM (SELECT 1 " + from + ") grouped")
                .params(params).query(Long.class).single();
        String order = "name,asc".equals(query.sort())
                ? "mi.name ASC" : "quantity_base DESC, mi.name ASC";
        List<InventoryConsumptionRow> rows = jdbc.sql("""
                SELECT c.restaurant_id, r.name restaurant_name, c.master_ingredient_id, mi.name,
                       SUM(c.quantity_base) quantity_base,
                       SUM(CASE WHEN c.unit_cost_snapshot IS NOT NULL
                           THEN c.quantity_base * c.unit_cost_snapshot END) historical_cost,
                       BOOL_AND(c.unit_cost_snapshot IS NOT NULL) cost_complete
                """ + from + " ORDER BY " + order + " LIMIT :limit OFFSET :offset")
                .params(params)
                .query((rs, rowNum) -> new InventoryConsumptionRow(
                        uuid(rs, "restaurant_id"), rs.getString("restaurant_name"),
                        uuid(rs, "master_ingredient_id"), rs.getString("name"),
                        money(rs.getBigDecimal("quantity_base")), rs.getBigDecimal("historical_cost"),
                        rs.getBoolean("cost_complete")))
                .list();
        return new ReportPage<>(rows, count, query.page(), query.size());
    }

    @Override
    public InventoryValuationReport inventoryValuation(ReportQuery query) {
        Map<String, Object> params = baseParams(query);
        record Total(BigDecimal known, int valued, int missing) {}
        Total total = jdbc.sql("""
                SELECT COALESCE(SUM(CASE WHEN si.id IS NOT NULL
                            THEN s.current_stock * si.last_cost_base ELSE 0 END), 0) known_value,
                       COUNT(*) FILTER (WHERE si.id IS NOT NULL) valued_count,
                       COUNT(*) FILTER (WHERE si.id IS NULL) missing_count
                FROM ingredient_stocks s
                JOIN master_ingredients mi ON mi.id = s.master_ingredient_id
                LEFT JOIN supplier_items si
                       ON si.id = mi.active_supplier_item_id AND si.deleted_at IS NULL
                WHERE s.restaurant_id IN (:restaurantIds)
                  AND s.deleted_at IS NULL
                """)
                .params(params)
                .query((rs, rowNum) -> new Total(
                        money(rs.getBigDecimal("known_value")), rs.getInt("valued_count"),
                        rs.getInt("missing_count")))
                .single();
        List<InventoryValuationReport.MissingCostIngredient> missing = jdbc.sql("""
                SELECT s.restaurant_id, r.name restaurant_name, mi.id, mi.name, s.current_stock
                FROM ingredient_stocks s
                JOIN restaurants r ON r.id = s.restaurant_id
                JOIN master_ingredients mi ON mi.id = s.master_ingredient_id
                LEFT JOIN supplier_items si
                       ON si.id = mi.active_supplier_item_id AND si.deleted_at IS NULL
                WHERE s.restaurant_id IN (:restaurantIds)
                  AND s.deleted_at IS NULL
                  AND si.id IS NULL
                ORDER BY r.name, mi.name
                LIMIT 100
                """)
                .params(params)
                .query((rs, rowNum) -> new InventoryValuationReport.MissingCostIngredient(
                        uuid(rs, "restaurant_id"), rs.getString("restaurant_name"), uuid(rs, "id"),
                        rs.getString("name"), money(rs.getBigDecimal("current_stock"))))
                .list();
        return new InventoryValuationReport(total.known(), total.valued(), total.missing(), missing);
    }

    @Override
    public ReportPage<LowStockReportRow> lowStock(ReportQuery query) {
        Map<String, Object> params = pageParams(query);
        String search = searchClause(query.search(), "mi.name", "r.name");
        String from = """
                FROM ingredient_stocks s
                JOIN master_ingredients mi ON mi.id = s.master_ingredient_id
                JOIN restaurants r ON r.id = s.restaurant_id
                WHERE s.restaurant_id IN (:restaurantIds)
                  AND s.deleted_at IS NULL
                  AND s.current_stock <= s.min_stock
                """ + search;
        long count = count(from, params);
        List<LowStockReportRow> rows = jdbc.sql("""
                SELECT s.restaurant_id, r.name restaurant_name, mi.id, mi.name,
                       s.current_stock, s.min_stock,
                       GREATEST(s.min_stock - s.current_stock, 0) shortage
                """ + from + """
                ORDER BY shortage DESC, mi.name
                LIMIT :limit OFFSET :offset
                """)
                .params(params)
                .query((rs, rowNum) -> new LowStockReportRow(
                        uuid(rs, "restaurant_id"), rs.getString("restaurant_name"), uuid(rs, "id"),
                        rs.getString("name"), money(rs.getBigDecimal("current_stock")),
                        money(rs.getBigDecimal("min_stock")), money(rs.getBigDecimal("shortage"))))
                .list();
        return new ReportPage<>(rows, count, query.page(), query.size());
    }

    private boolean hasOpenDay(ReportQuery query) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM restaurant_business_days
                    WHERE restaurant_id IN (:restaurantIds)
                      AND business_date BETWEEN :dateFrom AND :dateTo
                      AND status = 'OPEN'
                )
                """)
                .params(baseParams(query))
                .query(Boolean.class)
                .single();
    }

    private Map<String, Object> baseParams(ReportQuery query) {
        Map<String, Object> params = new HashMap<>();
        params.put("restaurantIds", query.scope().restaurantIds());
        params.put("dateFrom", query.dateFrom());
        params.put("dateTo", query.dateTo());
        if (query.search() != null && !query.search().isBlank()) {
            params.put("search", "%" + query.search().trim().toLowerCase() + "%");
        }
        return params;
    }

    private Map<String, Object> pageParams(ReportQuery query) {
        Map<String, Object> params = baseParams(query);
        params.put("limit", query.size());
        params.put("offset", query.page() * query.size());
        return params;
    }

    private String searchClause(String search, String... columns) {
        if (search == null || search.isBlank()) {
            return "";
        }
        return " AND (" + java.util.Arrays.stream(columns)
                .map(column -> "LOWER(" + column + ") LIKE :search")
                .collect(java.util.stream.Collectors.joining(" OR ")) + ") ";
    }

    private long count(String from, Map<String, Object> params) {
        Long value = jdbc.sql("SELECT COUNT(*) " + from).params(params).query(Long.class).single();
        return value == null ? 0 : value;
    }

    private String grouping(ReportGrouping grouping) {
        return switch (grouping == null ? ReportGrouping.DAY : grouping) {
            case DAY -> "day";
            case WEEK -> "week";
            case MONTH -> "month";
        };
    }

    private String sortBusinessDay(String sort) {
        return "date,asc".equals(sort) ? "bd.business_date ASC" : "bd.business_date DESC";
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private UUID uuid(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        return rs.getObject(column, UUID.class);
    }
}
