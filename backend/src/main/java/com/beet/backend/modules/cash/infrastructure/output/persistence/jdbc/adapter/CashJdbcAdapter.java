package com.beet.backend.modules.cash.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.cash.domain.exception.CashSessionConflictException;
import com.beet.backend.modules.cash.domain.model.CashRegisterDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionStatus;
import com.beet.backend.modules.cash.domain.model.CashSessionSummary;
import com.beet.backend.modules.cash.domain.spi.CashRegisterPersistencePort;
import com.beet.backend.modules.cash.domain.spi.CashSessionPersistencePort;
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
public class CashJdbcAdapter implements CashRegisterPersistencePort, CashSessionPersistencePort {

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
                    (restaurant_id, cash_register_id, status, opened_by,
                     opened_device_id, opening_amount, notes)
                VALUES
                    (:restaurantId, :cashRegisterId, :status::cash_session_status, :openedBy,
                     :openedDeviceId, :openingAmount, :notes)
                RETURNING *
                """)
                .param("restaurantId", session.getRestaurantId())
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
                       r.name AS restaurant_name
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

    private CashSessionDomain mapSession(ResultSet rs, int rowNum) throws SQLException {
        return CashSessionDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
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
                rs.getString("notes"));
    }
}
