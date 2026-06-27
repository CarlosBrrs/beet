package com.beet.backend.modules.table.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.table.domain.model.RestaurantTableDomain;
import com.beet.backend.modules.table.domain.model.TableAvailabilityStatus;
import com.beet.backend.modules.table.domain.spi.RestaurantTablePersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RestaurantTableJdbcAdapter implements RestaurantTablePersistencePort {
    private final JdbcClient jdbc;

    @Override
    public RestaurantTableDomain save(RestaurantTableDomain table) {
        return jdbc.sql("""
                INSERT INTO restaurant_tables
                    (restaurant_id, name, capacity, area, sort_order, is_active, notes, created_by, updated_by)
                VALUES
                    (:restaurantId, :name, :capacity, :area, :sortOrder, :isActive, :notes, :createdBy, :updatedBy)
                RETURNING *
                """)
                .param("restaurantId", table.getRestaurantId())
                .param("name", table.getName())
                .param("capacity", table.getCapacity())
                .param("area", table.getArea())
                .param("sortOrder", table.getSortOrder())
                .param("isActive", table.getIsActive())
                .param("notes", table.getNotes())
                .param("createdBy", table.getCreatedBy())
                .param("updatedBy", table.getUpdatedBy())
                .query(this::mapTable)
                .single();
    }

    @Override
    public RestaurantTableDomain update(RestaurantTableDomain table) {
        return jdbc.sql("""
                UPDATE restaurant_tables
                   SET name = :name,
                       capacity = :capacity,
                       area = :area,
                       sort_order = :sortOrder,
                       is_active = :isActive,
                       notes = :notes,
                       updated_at = NOW(),
                       updated_by = :updatedBy
                 WHERE id = :id
             RETURNING *
                """)
                .param("id", table.getId())
                .param("name", table.getName())
                .param("capacity", table.getCapacity())
                .param("area", table.getArea())
                .param("sortOrder", table.getSortOrder())
                .param("isActive", table.getIsActive())
                .param("notes", table.getNotes())
                .param("updatedBy", table.getUpdatedBy())
                .query(this::mapTable)
                .single();
    }

    @Override
    public Optional<RestaurantTableDomain> findByIdForUpdate(UUID id) {
        return jdbc.sql("SELECT * FROM restaurant_tables WHERE id = :id FOR UPDATE")
                .param("id", id)
                .query(this::mapTable)
                .optional();
    }

    @Override
    public List<RestaurantTableDomain> findByRestaurantId(UUID restaurantId) {
        return jdbc.sql("""
                SELECT rt.*, open_order.id AS open_order_id
                  FROM restaurant_tables rt
                  LEFT JOIN orders open_order
                    ON open_order.restaurant_id = rt.restaurant_id
                   AND open_order.table_id = rt.id
                   AND open_order.order_status = 'OPEN'
                 WHERE rt.restaurant_id = :restaurantId
                 ORDER BY COALESCE(rt.area, ''), rt.sort_order, LOWER(rt.name)
                """)
                .param("restaurantId", restaurantId)
                .query(this::mapTable)
                .list();
    }

    @Override
    public boolean existsByName(UUID restaurantId, String name) {
        return jdbc.sql("""
                SELECT COUNT(1)
                  FROM restaurant_tables
                 WHERE restaurant_id = :restaurantId
                   AND LOWER(name) = LOWER(:name)
                """)
                .param("restaurantId", restaurantId)
                .param("name", name)
                .query(Integer.class)
                .single() > 0;
    }

    @Override
    public Optional<UUID> findOpenOrderId(UUID restaurantId, UUID tableId) {
        return jdbc.sql("""
                SELECT id
                  FROM orders
                 WHERE restaurant_id = :restaurantId
                   AND table_id = :tableId
                   AND order_status = 'OPEN'
                """)
                .param("restaurantId", restaurantId)
                .param("tableId", tableId)
                .query(UUID.class)
                .optional();
    }

    private RestaurantTableDomain mapTable(ResultSet rs, int rowNum) throws SQLException {
        UUID openOrderId = hasColumn(rs, "open_order_id")
                ? rs.getObject("open_order_id", UUID.class)
                : null;
        boolean isActive = rs.getBoolean("is_active");
        TableAvailabilityStatus status = !isActive
                ? TableAvailabilityStatus.INACTIVE
                : openOrderId != null ? TableAvailabilityStatus.OCCUPIED : TableAvailabilityStatus.AVAILABLE;
        return RestaurantTableDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .name(rs.getString("name"))
                .capacity(rs.getInt("capacity"))
                .area(rs.getString("area"))
                .sortOrder(rs.getInt("sort_order"))
                .isActive(isActive)
                .notes(rs.getString("notes"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .availabilityStatus(status)
                .openOrderId(openOrderId)
                .build();
    }

    private boolean hasColumn(ResultSet rs, String column) throws SQLException {
        for (int index = 1; index <= rs.getMetaData().getColumnCount(); index++) {
            if (column.equalsIgnoreCase(rs.getMetaData().getColumnLabel(index))) return true;
        }
        return false;
    }
}
