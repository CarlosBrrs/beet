package com.beet.backend.modules.cash.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.cash.domain.exception.CashSessionConflictException;
import com.beet.backend.modules.cash.domain.model.CashRegisterDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionStatus;
import com.beet.backend.modules.cash.domain.model.CashSessionSummary;
import com.beet.backend.modules.cash.domain.model.BusinessDayClosureDomain;
import com.beet.backend.modules.cash.domain.model.BusinessDayEventType;
import com.beet.backend.modules.cash.domain.model.BusinessDayStatus;
import com.beet.backend.modules.cash.domain.model.CashMovementDirection;
import com.beet.backend.modules.cash.domain.model.CashMovementDomain;
import com.beet.backend.modules.cash.domain.model.CashMovementReason;
import com.beet.backend.modules.cash.domain.model.CashMovementStatus;
import com.beet.backend.modules.cash.domain.model.CashSessionReconciliationDomain;
import com.beet.backend.modules.cash.domain.model.PaymentTotalSnapshot;
import com.beet.backend.modules.cash.domain.model.RestaurantBusinessDayDomain;
import com.beet.backend.modules.cash.domain.spi.CashOperationsPersistencePort;
import com.beet.backend.modules.cash.domain.spi.CashRegisterPersistencePort;
import com.beet.backend.modules.cash.domain.spi.CashSessionPersistencePort;
import com.beet.backend.modules.order.domain.model.PaymentMethodType;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CashJdbcAdapter implements CashRegisterPersistencePort, CashSessionPersistencePort,
        CashOperationsPersistencePort {

    private final JdbcClient jdbc;

    @Override
    public CashRegisterDomain save(CashRegisterDomain register) {
        return jdbc.sql("""
                INSERT INTO cash_registers
                    (restaurant_id, name, device_id, is_active, notes, created_by, updated_by)
                VALUES
                    (:restaurantId, :name, :deviceId, :isActive, :notes, :createdBy, :updatedBy)
                RETURNING *
                """)
                .param("restaurantId", register.getRestaurantId())
                .param("name", register.getName())
                .param("deviceId", register.getDeviceId())
                .param("isActive", register.getIsActive())
                .param("notes", register.getNotes())
                .param("createdBy", register.getCreatedBy())
                .param("updatedBy", register.getUpdatedBy())
                .query(this::mapRegister)
                .single();
    }

    @Override
    public CashRegisterDomain update(CashRegisterDomain register) {
        return jdbc.sql("""
                UPDATE cash_registers
                   SET name = :name,
                       device_id = :deviceId,
                       is_active = :isActive,
                       notes = :notes,
                       updated_at = NOW(),
                       updated_by = :updatedBy
                 WHERE id = :id
             RETURNING *
                """)
                .param("id", register.getId())
                .param("name", register.getName())
                .param("deviceId", register.getDeviceId())
                .param("isActive", register.getIsActive())
                .param("notes", register.getNotes())
                .param("updatedBy", register.getUpdatedBy())
                .query(this::mapRegister)
                .single();
    }

    @Override
    public Optional<CashRegisterDomain> findRegisterById(UUID id) {
        return jdbc.sql("SELECT * FROM cash_registers WHERE id = :id")
                .param("id", id)
                .query(this::mapRegister)
                .optional();
    }

    @Override
    public Optional<CashRegisterDomain> findRegisterByIdForUpdate(UUID id) {
        return jdbc.sql("SELECT * FROM cash_registers WHERE id = :id FOR UPDATE")
                .param("id", id)
                .query(this::mapRegister)
                .optional();
    }

    @Override
    public List<CashRegisterDomain> findByRestaurantId(UUID restaurantId) {
        return jdbc.sql("""
                SELECT * FROM cash_registers
                 WHERE restaurant_id = :restaurantId
                 ORDER BY name ASC
                """)
                .param("restaurantId", restaurantId)
                .query(this::mapRegister)
                .list();
    }

    @Override
    public boolean existsByName(UUID restaurantId, String name) {
        return jdbc.sql("""
                SELECT COUNT(1) FROM cash_registers
                 WHERE restaurant_id = :restaurantId
                   AND LOWER(name) = LOWER(:name)
                """)
                .param("restaurantId", restaurantId)
                .param("name", name)
                .query(Integer.class)
                .single() > 0;
    }

    @Override
    public CashSessionDomain create(CashSessionDomain session) {
        return jdbc.sql("""
                INSERT INTO cash_sessions
                    (restaurant_id, business_day_id, cash_register_id, status, opened_by,
                     opened_device_id, opening_amount, notes)
                VALUES
                    (:restaurantId, :businessDayId, :cashRegisterId, :status::cash_session_status, :openedBy,
                     :openedDeviceId, :openingAmount, :notes)
                RETURNING *
                """)
                .param("restaurantId", session.getRestaurantId())
                .param("businessDayId", session.getBusinessDayId())
                .param("cashRegisterId", session.getCashRegisterId())
                .param("status", session.getStatus().name())
                .param("openedBy", session.getOpenedBy())
                .param("openedDeviceId", session.getOpenedDeviceId())
                .param("openingAmount", session.getOpeningAmount())
                .param("notes", session.getNotes())
                .query(this::mapSession)
                .single();
    }

    @Override
    public CashSessionDomain close(UUID sessionId, UUID closedBy, UUID closedDeviceId,
            BigDecimal closingAmount, String notes) {
        return jdbc.sql("""
                UPDATE cash_sessions
                   SET status = 'CLOSED',
                       closed_at = NOW(),
                       closed_by = :closedBy,
                       closed_device_id = :closedDeviceId,
                       closing_amount = :closingAmount,
                       notes = COALESCE(:notes, notes)
                 WHERE id = :id
                   AND status = 'OPEN'
             RETURNING *
                """)
                .param("id", sessionId)
                .param("closedBy", closedBy)
                .param("closedDeviceId", closedDeviceId)
                .param("closingAmount", closingAmount)
                .param("notes", notes)
                .query(this::mapSession)
                .optional()
                .orElseThrow(CashSessionConflictException::alreadyClosed);
    }

    @Override
    public Optional<CashSessionDomain> findSessionById(UUID id) {
        return jdbc.sql("SELECT * FROM cash_sessions WHERE id = :id")
                .param("id", id)
                .query(this::mapSession)
                .optional();
    }

    @Override
    public Optional<CashSessionDomain> findSessionByIdForUpdate(UUID id) {
        return jdbc.sql("SELECT * FROM cash_sessions WHERE id = :id FOR UPDATE")
                .param("id", id)
                .query(this::mapSession)
                .optional();
    }

    @Override
    public void lockSession(UUID restaurantId, UUID sessionId) {
        jdbc.sql("""
                SELECT id
                  FROM cash_sessions
                 WHERE id = :sessionId
                   AND restaurant_id = :restaurantId
                 FOR UPDATE
                """)
                .param("sessionId", sessionId)
                .param("restaurantId", restaurantId)
                .query(UUID.class)
                .optional();
    }

    @Override
    public Optional<RestaurantBusinessDayDomain> findOpenBusinessDay(UUID restaurantId) {
        return jdbc.sql("""
                SELECT * FROM restaurant_business_days
                 WHERE restaurant_id = :restaurantId
                   AND status = 'OPEN'
                """)
                .param("restaurantId", restaurantId)
                .query(this::mapBusinessDay)
                .optional();
    }

    @Override
    public Optional<RestaurantBusinessDayDomain> findBusinessDay(UUID restaurantId, UUID businessDayId) {
        return jdbc.sql("""
                SELECT * FROM restaurant_business_days
                 WHERE id = :businessDayId AND restaurant_id = :restaurantId
                """)
                .param("businessDayId", businessDayId)
                .param("restaurantId", restaurantId)
                .query(this::mapBusinessDay)
                .optional();
    }

    @Override
    public Optional<RestaurantBusinessDayDomain> findBusinessDayByDate(UUID restaurantId, LocalDate businessDate) {
        return jdbc.sql("""
                SELECT * FROM restaurant_business_days
                 WHERE restaurant_id = :restaurantId AND business_date = :businessDate
                """)
                .param("restaurantId", restaurantId)
                .param("businessDate", businessDate)
                .query(this::mapBusinessDay)
                .optional();
    }

    @Override
    public List<RestaurantBusinessDayDomain> findBusinessDays(UUID restaurantId) {
        return jdbc.sql("""
                SELECT * FROM restaurant_business_days
                 WHERE restaurant_id = :restaurantId
                 ORDER BY business_date DESC
                """)
                .param("restaurantId", restaurantId)
                .query(this::mapBusinessDay)
                .list();
    }

    @Override
    public RestaurantBusinessDayDomain createBusinessDay(RestaurantBusinessDayDomain day) {
        return jdbc.sql("""
                INSERT INTO restaurant_business_days
                    (restaurant_id, business_date, time_zone_snapshot, status, opened_by)
                VALUES (:restaurantId, :businessDate, :timeZone, 'OPEN', :openedBy)
                RETURNING *
                """)
                .param("restaurantId", day.getRestaurantId())
                .param("businessDate", day.getBusinessDate())
                .param("timeZone", day.getTimeZoneSnapshot())
                .param("openedBy", day.getOpenedBy())
                .query(this::mapBusinessDay)
                .single();
    }

    @Override
    public RestaurantBusinessDayDomain reopenBusinessDay(UUID businessDayId, UUID userId) {
        return jdbc.sql("""
                UPDATE restaurant_business_days
                   SET status = 'OPEN', closed_at = NULL, closed_by = NULL, updated_at = NOW()
                 WHERE id = :businessDayId AND status = 'CLOSED'
                RETURNING *
                """)
                .param("businessDayId", businessDayId)
                .query(this::mapBusinessDay)
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Business day is not closed."));
    }

    @Override
    public RestaurantBusinessDayDomain closeBusinessDay(UUID businessDayId, UUID userId) {
        return jdbc.sql("""
                UPDATE restaurant_business_days
                   SET status = 'CLOSED', closed_at = NOW(), closed_by = :userId, updated_at = NOW()
                 WHERE id = :businessDayId AND status = 'OPEN'
                RETURNING *
                """)
                .param("businessDayId", businessDayId)
                .param("userId", userId)
                .query(this::mapBusinessDay)
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Business day is not open."));
    }

    @Override
    public void saveBusinessDayEvent(UUID restaurantId, UUID businessDayId, BusinessDayEventType type,
            UUID userId, String reason) {
        jdbc.sql("""
                INSERT INTO business_day_events
                    (restaurant_id, business_day_id, event_type, reason, occurred_by)
                VALUES (:restaurantId, :businessDayId, :eventType::business_day_event_type, :reason, :userId)
                """)
                .param("restaurantId", restaurantId)
                .param("businessDayId", businessDayId)
                .param("eventType", type.name())
                .param("reason", reason)
                .param("userId", userId)
                .update();
    }

    @Override
    public RestaurantBusinessDayDomain loadBusinessDayBlockers(UUID restaurantId, UUID businessDayId) {
        RestaurantBusinessDayDomain day = findBusinessDay(restaurantId, businessDayId)
                .orElseThrow(() -> new IllegalArgumentException("Business day not found."));
        Map<String, Object> counts = jdbc.sql("""
                SELECT
                    (SELECT COUNT(*) FROM cash_sessions
                      WHERE business_day_id = :businessDayId AND status = 'OPEN') AS open_sessions,
                    (SELECT COUNT(*) FROM orders
                      WHERE business_day_id = :businessDayId
                        AND order_status IN ('DRAFT', 'AWAITING_PAYMENT', 'OPEN')) AS pending_orders,
                    (SELECT COUNT(*) FROM cash_sessions cs
                      WHERE cs.business_day_id = :businessDayId
                        AND cs.status = 'CLOSED'
                        AND NOT EXISTS (
                            SELECT 1 FROM cash_session_closures c WHERE c.cash_session_id = cs.id
                        )) AS missing_reconciliations,
                    (SELECT COUNT(*) FROM cash_session_closures
                      WHERE business_day_id = :businessDayId
                        AND difference_amount <> 0
                        AND NULLIF(BTRIM(difference_reason), '') IS NULL) AS unexplained_differences,
                    (SELECT COUNT(*) FROM orders
                      WHERE business_day_id = :businessDayId
                        AND refund_due_snapshot > 0) AS pending_refunds
                """)
                .param("businessDayId", businessDayId)
                .query((rs, rowNum) -> Map.<String, Object>of(
                        "openSessions", rs.getInt("open_sessions"),
                        "pendingOrders", rs.getInt("pending_orders") + rs.getInt("pending_refunds"),
                        "missing", rs.getInt("missing_reconciliations"),
                        "unexplained", rs.getInt("unexplained_differences")))
                .single();
        day.setOpenSessionCount((Integer) counts.get("openSessions"));
        day.setPendingOrderCount((Integer) counts.get("pendingOrders"));
        day.setMissingReconciliationCount((Integer) counts.get("missing"));
        day.setUnexplainedDifferenceCount((Integer) counts.get("unexplained"));
        return day;
    }

    @Override
    public List<PaymentTotalSnapshot> calculateBusinessDayPaymentTotals(UUID businessDayId) {
        return paymentTotals("""
                JOIN cash_sessions cs ON cs.id = p.cash_session_id
                WHERE cs.business_day_id = :scopeId
                """, businessDayId);
    }

    @Override
    public BigDecimal sumBusinessDayMovements(UUID businessDayId, String direction) {
        return jdbc.sql("""
                SELECT COALESCE(SUM(amount), 0)
                  FROM cash_movements
                 WHERE business_day_id = :businessDayId
                   AND status = 'RECORDED'
                   AND direction = :direction::cash_movement_direction
                """)
                .param("businessDayId", businessDayId)
                .param("direction", direction)
                .query(BigDecimal.class)
                .single();
    }

    @Override
    public BigDecimal sumBusinessDayCountedCash(UUID businessDayId) {
        return jdbc.sql("""
                SELECT COALESCE(SUM(counted_cash), 0)
                  FROM cash_session_closures
                 WHERE business_day_id = :businessDayId
                """)
                .param("businessDayId", businessDayId)
                .query(BigDecimal.class)
                .single();
    }

    @Override
    public BigDecimal sumBusinessDayExpectedCash(UUID businessDayId) {
        return jdbc.sql("""
                SELECT COALESCE(SUM(expected_cash), 0)
                  FROM cash_session_closures
                 WHERE business_day_id = :businessDayId
                """)
                .param("businessDayId", businessDayId)
                .query(BigDecimal.class)
                .single();
    }

    @Override
    public BusinessDayClosureDomain createBusinessDayClosure(BusinessDayClosureDomain closure) {
        int sequence = jdbc.sql("""
                SELECT COALESCE(MAX(closure_sequence), 0) + 1
                  FROM business_day_closures
                 WHERE business_day_id = :businessDayId
                """)
                .param("businessDayId", closure.getBusinessDayId())
                .query(Integer.class)
                .single();
        BusinessDayClosureDomain saved = jdbc.sql("""
                INSERT INTO business_day_closures
                    (restaurant_id, business_day_id, closure_sequence, payments_total, tips_total,
                     refunds_total, cash_in_total, cash_out_total, expected_cash_total,
                     counted_cash_total, difference_total, notes, closed_by)
                VALUES
                    (:restaurantId, :businessDayId, :sequence, :payments, :tips,
                     :refunds, :cashIn, :cashOut, :expected, :counted, :difference, :notes, :closedBy)
                RETURNING *
                """)
                .param("restaurantId", closure.getRestaurantId())
                .param("businessDayId", closure.getBusinessDayId())
                .param("sequence", sequence)
                .param("payments", closure.getPaymentsTotal())
                .param("tips", closure.getTipsTotal())
                .param("refunds", closure.getRefundsTotal())
                .param("cashIn", closure.getCashInTotal())
                .param("cashOut", closure.getCashOutTotal())
                .param("expected", closure.getExpectedCashTotal())
                .param("counted", closure.getCountedCashTotal())
                .param("difference", closure.getDifferenceTotal())
                .param("notes", closure.getNotes())
                .param("closedBy", closure.getClosedBy())
                .query(this::mapBusinessDayClosure)
                .single();
        savePaymentTotals("business_day_payment_totals", "closure_id", saved.getId(), closure.getPaymentTotals());
        saved = BusinessDayClosureDomain.builder()
                .id(saved.getId())
                .restaurantId(saved.getRestaurantId())
                .businessDayId(saved.getBusinessDayId())
                .closureSequence(saved.getClosureSequence())
                .paymentsTotal(saved.getPaymentsTotal())
                .tipsTotal(saved.getTipsTotal())
                .refundsTotal(saved.getRefundsTotal())
                .cashInTotal(saved.getCashInTotal())
                .cashOutTotal(saved.getCashOutTotal())
                .expectedCashTotal(saved.getExpectedCashTotal())
                .countedCashTotal(saved.getCountedCashTotal())
                .differenceTotal(saved.getDifferenceTotal())
                .notes(saved.getNotes())
                .closedAt(saved.getClosedAt())
                .closedBy(saved.getClosedBy())
                .paymentTotals(closure.getPaymentTotals())
                .build();
        return saved;
    }

    @Override
    public List<CashMovementDomain> findMovements(UUID restaurantId, UUID sessionId) {
        return jdbc.sql("""
                SELECT * FROM cash_movements
                 WHERE restaurant_id = :restaurantId AND cash_session_id = :sessionId
                 ORDER BY created_at DESC
                """)
                .param("restaurantId", restaurantId)
                .param("sessionId", sessionId)
                .query(this::mapMovement)
                .list();
    }

    @Override
    public CashMovementDomain saveMovement(CashMovementDomain movement) {
        return jdbc.sql("""
                INSERT INTO cash_movements
                    (restaurant_id, business_day_id, cash_session_id, direction, reason, amount,
                     status, notes, created_by, created_device_id)
                VALUES
                    (:restaurantId, :businessDayId, :sessionId, :direction::cash_movement_direction,
                     :reason::cash_movement_reason, :amount, 'RECORDED', :notes, :createdBy, :deviceId)
                RETURNING *
                """)
                .param("restaurantId", movement.getRestaurantId())
                .param("businessDayId", movement.getBusinessDayId())
                .param("sessionId", movement.getCashSessionId())
                .param("direction", movement.getDirection().name())
                .param("reason", movement.getReason().name())
                .param("amount", movement.getAmount())
                .param("notes", movement.getNotes())
                .param("createdBy", movement.getCreatedBy())
                .param("deviceId", movement.getCreatedDeviceId())
                .query(this::mapMovement)
                .single();
    }

    @Override
    public Optional<CashMovementDomain> findMovement(UUID restaurantId, UUID sessionId, UUID movementId) {
        return jdbc.sql("""
                SELECT * FROM cash_movements
                 WHERE id = :movementId AND restaurant_id = :restaurantId AND cash_session_id = :sessionId
                """)
                .param("movementId", movementId)
                .param("restaurantId", restaurantId)
                .param("sessionId", sessionId)
                .query(this::mapMovement)
                .optional();
    }

    @Override
    public CashMovementDomain voidMovement(UUID movementId, UUID userId, UUID deviceId, String reason) {
        return jdbc.sql("""
                UPDATE cash_movements
                   SET status = 'VOIDED', voided_at = NOW(), voided_by = :userId,
                       voided_device_id = :deviceId, void_reason = :reason
                 WHERE id = :movementId AND status = 'RECORDED'
                RETURNING *
                """)
                .param("movementId", movementId)
                .param("userId", userId)
                .param("deviceId", deviceId)
                .param("reason", reason)
                .query(this::mapMovement)
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Cash movement is already voided."));
    }

    @Override
    public CashSessionReconciliationDomain calculateSessionReconciliation(UUID restaurantId, UUID sessionId) {
        CashSessionDomain session = findSessionById(sessionId)
                .filter(value -> value.getRestaurantId().equals(restaurantId))
                .orElseThrow(() -> new IllegalArgumentException("Cash session not found."));
        List<PaymentTotalSnapshot> totals = paymentTotals(
                "WHERE p.cash_session_id = :scopeId", sessionId);
        BigDecimal payments = totals.stream().map(PaymentTotalSnapshot::paymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tips = totals.stream().map(PaymentTotalSnapshot::tipAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refunds = totals.stream().map(PaymentTotalSnapshot::refundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        PaymentTotalSnapshot cash = totals.stream()
                .filter(total -> total.methodType() == PaymentMethodType.CASH)
                .findFirst()
                .orElse(null);
        BigDecimal cashIn = sumSessionMovements(sessionId, "IN");
        BigDecimal cashOut = sumSessionMovements(sessionId, "OUT");
        BigDecimal expected = session.getOpeningAmount()
                .add(cash == null ? BigDecimal.ZERO : cash.paymentAmount())
                .add(cash == null ? BigDecimal.ZERO : cash.tipAmount())
                .subtract(cash == null ? BigDecimal.ZERO : cash.refundAmount())
                .add(cashIn)
                .subtract(cashOut);
        return CashSessionReconciliationDomain.builder()
                .restaurantId(restaurantId)
                .businessDayId(session.getBusinessDayId())
                .cashSessionId(sessionId)
                .openingAmount(session.getOpeningAmount())
                .paymentsTotal(payments)
                .tipsTotal(tips)
                .refundsTotal(refunds)
                .cashInTotal(cashIn)
                .cashOutTotal(cashOut)
                .expectedCash(expected)
                .closed(false)
                .paymentTotals(totals)
                .build();
    }

    @Override
    public Optional<CashSessionReconciliationDomain> findSessionClosure(UUID restaurantId, UUID sessionId) {
        Optional<CashSessionReconciliationDomain> closure = jdbc.sql("""
                SELECT * FROM cash_session_closures
                 WHERE restaurant_id = :restaurantId AND cash_session_id = :sessionId
                """)
                .param("restaurantId", restaurantId)
                .param("sessionId", sessionId)
                .query(this::mapSessionClosure)
                .optional();
        closure.ifPresent(value -> value.setPaymentTotals(loadClosurePaymentTotals(value.getId())));
        return closure;
    }

    @Override
    public CashSessionReconciliationDomain saveSessionClosure(CashSessionReconciliationDomain value) {
        CashSessionReconciliationDomain saved = jdbc.sql("""
                INSERT INTO cash_session_closures
                    (restaurant_id, business_day_id, cash_session_id, opening_amount,
                     payments_total, tips_total, refunds_total, cash_in_total, cash_out_total,
                     expected_cash, counted_cash, difference_amount, difference_reason, notes,
                     closed_by, closed_device_id)
                VALUES
                    (:restaurantId, :businessDayId, :sessionId, :opening,
                     :payments, :tips, :refunds, :cashIn, :cashOut,
                     :expected, :counted, :difference, :differenceReason, :notes,
                     :closedBy, :closedDeviceId)
                RETURNING *
                """)
                .param("restaurantId", value.getRestaurantId())
                .param("businessDayId", value.getBusinessDayId())
                .param("sessionId", value.getCashSessionId())
                .param("opening", value.getOpeningAmount())
                .param("payments", value.getPaymentsTotal())
                .param("tips", value.getTipsTotal())
                .param("refunds", value.getRefundsTotal())
                .param("cashIn", value.getCashInTotal())
                .param("cashOut", value.getCashOutTotal())
                .param("expected", value.getExpectedCash())
                .param("counted", value.getCountedCash())
                .param("difference", value.getDifferenceAmount())
                .param("differenceReason", value.getDifferenceReason())
                .param("notes", value.getNotes())
                .param("closedBy", value.getClosedBy())
                .param("closedDeviceId", value.getClosedDeviceId())
                .query(this::mapSessionClosure)
                .single();
        savePaymentTotals("cash_session_payment_totals", "closure_id", saved.getId(), value.getPaymentTotals());
        saved.setPaymentTotals(value.getPaymentTotals());
        return saved;
    }

    private BigDecimal sumSessionMovements(UUID sessionId, String direction) {
        return jdbc.sql("""
                SELECT COALESCE(SUM(amount), 0) FROM cash_movements
                 WHERE cash_session_id = :sessionId AND status = 'RECORDED'
                   AND direction = :direction::cash_movement_direction
                """)
                .param("sessionId", sessionId)
                .param("direction", direction)
                .query(BigDecimal.class)
                .single();
    }

    private List<PaymentTotalSnapshot> paymentTotals(String scopeSql, UUID scopeId) {
        return jdbc.sql("""
                WITH refunds AS (
                    SELECT pr.payment_id, SUM(pr.amount) AS amount
                      FROM payment_refunds pr
                     WHERE pr.status = 'RECORDED'
                     GROUP BY pr.payment_id
                )
                SELECT pm.id, pm.code, pm.name, pm.type,
                       COUNT(p.id) AS payment_count,
                       COALESCE(SUM(p.amount), 0) AS payment_amount,
                       COALESCE(SUM(p.tip_amount), 0) AS tip_amount,
                       COALESCE(SUM(r.amount), 0) AS refund_amount
                  FROM payments p
                  JOIN payment_methods pm ON pm.id = p.payment_method_id
                  LEFT JOIN refunds r ON r.payment_id = p.id
                """ + scopeSql + """
                   AND p.status = 'RECORDED'
                 GROUP BY pm.id, pm.code, pm.name, pm.type
                 ORDER BY pm.sort_order, pm.name
                """)
                .param("scopeId", scopeId)
                .query((rs, rowNum) -> {
                    BigDecimal payment = rs.getBigDecimal("payment_amount");
                    BigDecimal tip = rs.getBigDecimal("tip_amount");
                    BigDecimal refund = rs.getBigDecimal("refund_amount");
                    return new PaymentTotalSnapshot(
                            rs.getObject("id", UUID.class),
                            rs.getString("code"),
                            rs.getString("name"),
                            PaymentMethodType.valueOf(rs.getString("type")),
                            rs.getInt("payment_count"),
                            payment,
                            tip,
                            refund,
                            payment.add(tip).subtract(refund));
                })
                .list();
    }

    private void savePaymentTotals(String table, String ownerColumn, UUID ownerId,
            List<PaymentTotalSnapshot> totals) {
        for (PaymentTotalSnapshot total : totals) {
            jdbc.sql("""
                    INSERT INTO %s
                        (%s, payment_method_id, method_code_snapshot, method_name_snapshot,
                         method_type_snapshot, payment_count, payment_amount, tip_amount,
                         refund_amount, net_amount)
                    VALUES
                        (:ownerId, :methodId, :code, :name, :type::payment_method_type,
                         :count, :payment, :tip, :refund, :net)
                    """.formatted(table, ownerColumn))
                    .param("ownerId", ownerId)
                    .param("methodId", total.paymentMethodId())
                    .param("code", total.methodCode())
                    .param("name", total.methodName())
                    .param("type", total.methodType().name())
                    .param("count", total.paymentCount())
                    .param("payment", total.paymentAmount())
                    .param("tip", total.tipAmount())
                    .param("refund", total.refundAmount())
                    .param("net", total.netAmount())
                    .update();
        }
    }

    private List<PaymentTotalSnapshot> loadClosurePaymentTotals(UUID closureId) {
        return jdbc.sql("""
                SELECT * FROM cash_session_payment_totals
                 WHERE closure_id = :closureId ORDER BY method_name_snapshot
                """)
                .param("closureId", closureId)
                .query((rs, rowNum) -> new PaymentTotalSnapshot(
                        rs.getObject("payment_method_id", UUID.class),
                        rs.getString("method_code_snapshot"),
                        rs.getString("method_name_snapshot"),
                        PaymentMethodType.valueOf(rs.getString("method_type_snapshot")),
                        rs.getInt("payment_count"),
                        rs.getBigDecimal("payment_amount"),
                        rs.getBigDecimal("tip_amount"),
                        rs.getBigDecimal("refund_amount"),
                        rs.getBigDecimal("net_amount")))
                .list();
    }

    @Override
    public Optional<CashSessionDomain> findOpenByRegisterId(UUID cashRegisterId) {
        return jdbc.sql("""
                SELECT * FROM cash_sessions
                 WHERE cash_register_id = :cashRegisterId
                   AND status = 'OPEN'
                """)
                .param("cashRegisterId", cashRegisterId)
                .query(this::mapSession)
                .optional();
    }

    @Override
    public Optional<CashSessionDomain> findOpenByRestaurantAndDevice(UUID restaurantId, UUID deviceId) {
        return jdbc.sql("""
                SELECT * FROM cash_sessions
                 WHERE restaurant_id = :restaurantId
                   AND opened_device_id = :deviceId
                   AND status = 'OPEN'
                """)
                .param("restaurantId", restaurantId)
                .param("deviceId", deviceId)
                .query(this::mapSession)
                .optional();
    }

    @Override
    public PageResponse<CashSessionSummary> findSessionsPaged(
            List<UUID> restaurantIds,
            CashSessionStatus status,
            LocalDate from,
            LocalDate to,
            String timeZone,
            UUID cashRegisterId,
            int page,
            int size) {
        StringBuilder filters = new StringBuilder(" WHERE cs.restaurant_id IN (:restaurantIds)");
        Map<String, Object> params = new HashMap<>();
        params.put("restaurantIds", restaurantIds);

        if (status != null) {
            filters.append(" AND cs.status = CAST(:status AS cash_session_status)");
            params.put("status", status.name());
        }
        if (from != null) {
            filters.append(" AND (cs.opened_at AT TIME ZONE :timeZone) >= CAST(:fromDate AS DATE)");
            params.put("fromDate", from);
        }
        if (to != null) {
            filters.append(" AND (cs.opened_at AT TIME ZONE :timeZone) < CAST(:toDate AS DATE) + INTERVAL '1 day'");
            params.put("toDate", to);
        }
        if (from != null || to != null) {
            params.put("timeZone", timeZone);
        }
        if (cashRegisterId != null) {
            filters.append(" AND cs.cash_register_id = :cashRegisterId");
            params.put("cashRegisterId", cashRegisterId);
        }

        String fromClause = """
                 FROM cash_sessions cs
                 JOIN cash_registers cr ON cr.id = cs.cash_register_id
                 JOIN restaurants r ON r.id = cs.restaurant_id
                 LEFT JOIN cash_session_closures c ON c.cash_session_id = cs.id
                """ + filters;

        long totalElements = jdbc.sql("SELECT COUNT(1)" + fromClause)
                .params(params)
                .query(Long.class)
                .single();

        params.put("limit", size);
        params.put("offset", (long) page * size);
        List<CashSessionSummary> content = jdbc.sql("""
                SELECT cs.*,
                       cr.name AS cash_register_name,
                       r.name AS restaurant_name,
                       c.expected_cash,
                       c.difference_amount,
                       c.difference_reason
                """ + fromClause + """
                 ORDER BY cs.opened_at DESC
                 LIMIT :limit OFFSET :offset
                """)
                .params(params)
                .query(this::mapSessionSummary)
                .list();

        return PageResponse.of(content, totalElements, page, size);
    }

    private CashRegisterDomain mapRegister(ResultSet rs, int rowNum) throws SQLException {
        return CashRegisterDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .name(rs.getString("name"))
                .deviceId(rs.getObject("device_id", UUID.class))
                .isActive(rs.getBoolean("is_active"))
                .notes(rs.getString("notes"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .build();
    }

    private RestaurantBusinessDayDomain mapBusinessDay(ResultSet rs, int rowNum) throws SQLException {
        return RestaurantBusinessDayDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .businessDate(rs.getObject("business_date", LocalDate.class))
                .timeZoneSnapshot(rs.getString("time_zone_snapshot"))
                .status(BusinessDayStatus.valueOf(rs.getString("status")))
                .openedAt(rs.getObject("opened_at", OffsetDateTime.class))
                .openedBy(rs.getObject("opened_by", UUID.class))
                .closedAt(rs.getObject("closed_at", OffsetDateTime.class))
                .closedBy(rs.getObject("closed_by", UUID.class))
                .build();
    }

    private CashMovementDomain mapMovement(ResultSet rs, int rowNum) throws SQLException {
        return CashMovementDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .businessDayId(rs.getObject("business_day_id", UUID.class))
                .cashSessionId(rs.getObject("cash_session_id", UUID.class))
                .direction(CashMovementDirection.valueOf(rs.getString("direction")))
                .reason(CashMovementReason.valueOf(rs.getString("reason")))
                .amount(rs.getBigDecimal("amount"))
                .status(CashMovementStatus.valueOf(rs.getString("status")))
                .notes(rs.getString("notes"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .createdDeviceId(rs.getObject("created_device_id", UUID.class))
                .voidedAt(rs.getObject("voided_at", OffsetDateTime.class))
                .voidedBy(rs.getObject("voided_by", UUID.class))
                .voidedDeviceId(rs.getObject("voided_device_id", UUID.class))
                .voidReason(rs.getString("void_reason"))
                .build();
    }

    private CashSessionReconciliationDomain mapSessionClosure(ResultSet rs, int rowNum) throws SQLException {
        return CashSessionReconciliationDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .businessDayId(rs.getObject("business_day_id", UUID.class))
                .cashSessionId(rs.getObject("cash_session_id", UUID.class))
                .openingAmount(rs.getBigDecimal("opening_amount"))
                .paymentsTotal(rs.getBigDecimal("payments_total"))
                .tipsTotal(rs.getBigDecimal("tips_total"))
                .refundsTotal(rs.getBigDecimal("refunds_total"))
                .cashInTotal(rs.getBigDecimal("cash_in_total"))
                .cashOutTotal(rs.getBigDecimal("cash_out_total"))
                .expectedCash(rs.getBigDecimal("expected_cash"))
                .countedCash(rs.getBigDecimal("counted_cash"))
                .differenceAmount(rs.getBigDecimal("difference_amount"))
                .differenceReason(rs.getString("difference_reason"))
                .notes(rs.getString("notes"))
                .closedAt(rs.getObject("closed_at", OffsetDateTime.class))
                .closedBy(rs.getObject("closed_by", UUID.class))
                .closedDeviceId(rs.getObject("closed_device_id", UUID.class))
                .closed(true)
                .build();
    }

    private BusinessDayClosureDomain mapBusinessDayClosure(ResultSet rs, int rowNum) throws SQLException {
        return BusinessDayClosureDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .businessDayId(rs.getObject("business_day_id", UUID.class))
                .closureSequence(rs.getInt("closure_sequence"))
                .paymentsTotal(rs.getBigDecimal("payments_total"))
                .tipsTotal(rs.getBigDecimal("tips_total"))
                .refundsTotal(rs.getBigDecimal("refunds_total"))
                .cashInTotal(rs.getBigDecimal("cash_in_total"))
                .cashOutTotal(rs.getBigDecimal("cash_out_total"))
                .expectedCashTotal(rs.getBigDecimal("expected_cash_total"))
                .countedCashTotal(rs.getBigDecimal("counted_cash_total"))
                .differenceTotal(rs.getBigDecimal("difference_total"))
                .notes(rs.getString("notes"))
                .closedAt(rs.getObject("closed_at", OffsetDateTime.class))
                .closedBy(rs.getObject("closed_by", UUID.class))
                .build();
    }

    private CashSessionDomain mapSession(ResultSet rs, int rowNum) throws SQLException {
        return CashSessionDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .businessDayId(rs.getObject("business_day_id", UUID.class))
                .cashRegisterId(rs.getObject("cash_register_id", UUID.class))
                .status(CashSessionStatus.valueOf(rs.getString("status")))
                .openedAt(rs.getObject("opened_at", OffsetDateTime.class))
                .openedBy(rs.getObject("opened_by", UUID.class))
                .openedDeviceId(rs.getObject("opened_device_id", UUID.class))
                .openingAmount(rs.getBigDecimal("opening_amount"))
                .closedAt(rs.getObject("closed_at", OffsetDateTime.class))
                .closedBy(rs.getObject("closed_by", UUID.class))
                .closedDeviceId(rs.getObject("closed_device_id", UUID.class))
                .closingAmount(rs.getBigDecimal("closing_amount"))
                .notes(rs.getString("notes"))
                .build();
    }

    private CashSessionSummary mapSessionSummary(ResultSet rs, int rowNum) throws SQLException {
        return new CashSessionSummary(
                rs.getObject("id", UUID.class),
                rs.getObject("restaurant_id", UUID.class),
                rs.getString("restaurant_name"),
                rs.getObject("cash_register_id", UUID.class),
                rs.getString("cash_register_name"),
                CashSessionStatus.valueOf(rs.getString("status")),
                rs.getObject("opened_at", OffsetDateTime.class),
                rs.getObject("opened_by", UUID.class),
                rs.getObject("opened_device_id", UUID.class),
                rs.getBigDecimal("opening_amount"),
                rs.getObject("closed_at", OffsetDateTime.class),
                rs.getObject("closed_by", UUID.class),
                rs.getObject("closed_device_id", UUID.class),
                rs.getBigDecimal("closing_amount"),
                rs.getBigDecimal("expected_cash"),
                rs.getBigDecimal("difference_amount"),
                rs.getString("difference_reason"),
                rs.getString("notes"));
    }
}
