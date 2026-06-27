package com.beet.backend.modules.supplier.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.supplier.application.dto.SupplierResponse;
import com.beet.backend.modules.supplier.application.port.out.SupplierQueryPort;
import com.beet.backend.modules.supplier.domain.model.SupplierDomain;
import com.beet.backend.modules.supplier.domain.spi.SupplierPersistencePort;
import com.beet.backend.modules.supplier.infrastructure.output.persistence.jdbc.mapper.SupplierAggregateMapper;
import com.beet.backend.modules.supplier.infrastructure.output.persistence.jdbc.repository.SupplierJdbcRepository;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SupplierJdbcAdapter implements SupplierPersistencePort, SupplierQueryPort {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "name", "LOWER(name)",
            "documentNumber", "document_number",
            "email", "LOWER(email)",
            "phone", "phone",
            "isActive", "is_active");

    private final SupplierJdbcRepository repository;
    private final SupplierAggregateMapper mapper;
    private final JdbcClient jdbcClient;

    @Override
    public SupplierDomain save(SupplierDomain supplier) {
        if (supplier.getId() == null) {
            var saved = repository.save(mapper.toAggregate(supplier));
            return mapper.toDomain(saved);
        }
        return jdbcClient.sql("""
                        UPDATE suppliers
                           SET document_type_id = :documentTypeId,
                               document_number = :documentNumber,
                               name = :name,
                               contact_name = :contactName,
                               email = :email,
                               phone = :phone,
                               address = :address,
                               is_active = :isActive,
                               updated_at = NOW()
                         WHERE id = :id
                           AND owner_id = :ownerId
                           AND deleted_at IS NULL
                     RETURNING id, owner_id, document_type_id, document_number, name,
                               contact_name, email, phone, address, is_active
                        """)
                .param("id", supplier.getId())
                .param("ownerId", supplier.getOwnerId())
                .param("documentTypeId", supplier.getDocumentTypeId())
                .param("documentNumber", supplier.getDocumentNumber())
                .param("name", supplier.getName())
                .param("contactName", supplier.getContactName())
                .param("email", supplier.getEmail())
                .param("phone", supplier.getPhone())
                .param("address", supplier.getAddress())
                .param("isActive", supplier.getIsActive())
                .query((rs, rowNum) -> SupplierDomain.builder()
                        .id(rs.getObject("id", UUID.class))
                        .ownerId(rs.getObject("owner_id", UUID.class))
                        .documentTypeId(rs.getObject("document_type_id", UUID.class))
                        .documentNumber(rs.getString("document_number"))
                        .name(rs.getString("name"))
                        .contactName(rs.getString("contact_name"))
                        .email(rs.getString("email"))
                        .phone(rs.getString("phone"))
                        .address(rs.getString("address"))
                        .isActive(rs.getBoolean("is_active"))
                        .build())
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found."));
    }

    @Override
    public Optional<SupplierDomain> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<SupplierDomain> findByIdAndOwnerId(UUID id, UUID ownerId) {
        return jdbcClient.sql("""
                        SELECT *
                        FROM suppliers
                        WHERE id = :id
                          AND owner_id = :ownerId
                          AND deleted_at IS NULL
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .query((rs, rowNum) -> SupplierDomain.builder()
                        .id(rs.getObject("id", UUID.class))
                        .ownerId(rs.getObject("owner_id", UUID.class))
                        .documentTypeId(rs.getObject("document_type_id", UUID.class))
                        .documentNumber(rs.getString("document_number"))
                        .name(rs.getString("name"))
                        .contactName(rs.getString("contact_name"))
                        .email(rs.getString("email"))
                        .phone(rs.getString("phone"))
                        .address(rs.getString("address"))
                        .isActive(rs.getBoolean("is_active"))
                        .build())
                .optional();
    }

    @Override
    public boolean existsByOwnerAndDocument(UUID ownerId, UUID documentTypeId, String documentNumber) {
        Boolean exists = jdbcClient.sql("""
                        SELECT COUNT(1) > 0
                        FROM suppliers
                        WHERE owner_id = :ownerId
                          AND document_type_id = :documentTypeId
                          AND LOWER(document_number) = LOWER(:documentNumber)
                          AND deleted_at IS NULL
                        """)
                .param("ownerId", ownerId)
                .param("documentTypeId", documentTypeId)
                .param("documentNumber", documentNumber)
                .query(Boolean.class)
                .single();
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public boolean existsByOwnerAndDocumentExcludingId(
            UUID ownerId, UUID documentTypeId, String documentNumber, UUID excludedId) {
        Boolean exists = jdbcClient.sql("""
                        SELECT COUNT(1) > 0
                        FROM suppliers
                        WHERE owner_id = :ownerId
                          AND document_type_id = :documentTypeId
                          AND LOWER(document_number) = LOWER(:documentNumber)
                          AND id <> :excludedId
                          AND deleted_at IS NULL
                        """)
                .param("ownerId", ownerId)
                .param("documentTypeId", documentTypeId)
                .param("documentNumber", documentNumber)
                .param("excludedId", excludedId)
                .query(Boolean.class)
                .single();
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public boolean hasBlockingReferences(UUID supplierId) {
        Boolean exists = jdbcClient.sql("""
                        SELECT EXISTS (
                            SELECT 1 FROM supplier_items WHERE supplier_id = :supplierId AND deleted_at IS NULL
                            UNION ALL
                            SELECT 1 FROM invoices WHERE supplier_id = :supplierId
                        )
                        """)
                .param("supplierId", supplierId)
                .query(Boolean.class)
                .single();
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void updateActive(UUID supplierId, UUID ownerId, boolean isActive) {
        jdbcClient.sql("""
                        UPDATE suppliers
                           SET is_active = :isActive,
                               updated_at = NOW()
                         WHERE id = :supplierId
                           AND owner_id = :ownerId
                           AND deleted_at IS NULL
                        """)
                .param("supplierId", supplierId)
                .param("ownerId", ownerId)
                .param("isActive", isActive)
                .update();
    }

    @Override
    public void softDelete(UUID supplierId, UUID ownerId, UUID actorId) {
        jdbcClient.sql("""
                        UPDATE suppliers
                           SET deleted_at = NOW(),
                               deleted_by = :actorId,
                               updated_at = NOW(),
                               is_active = false
                         WHERE id = :supplierId
                           AND owner_id = :ownerId
                           AND deleted_at IS NULL
                        """)
                .param("supplierId", supplierId)
                .param("ownerId", ownerId)
                .param("actorId", actorId)
                .update();
    }

    @Override
    public List<SupplierResponse> findAllActiveByOwnerId(UUID ownerId) {
        return jdbcClient.sql("""
                    SELECT id, name, document_type_id, document_number, contact_name,
                           email, phone, address, is_active
                    FROM suppliers
                    WHERE owner_id = :ownerId
                      AND is_active = true
                      AND deleted_at IS NULL
                    ORDER BY name ASC
                """)
                .param("ownerId", ownerId)
                .query(this::mapSupplierResponse)
                .list();
    }

    @Override
    public PageResponse<SupplierResponse> findAllByOwnerId(
            UUID ownerId, int page, int size, String search, Boolean active, String sortBy, boolean sortDesc) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String orderColumn = SORT_COLUMNS.getOrDefault(sortBy, "LOWER(name)");
        String orderDir = sortDesc ? "DESC" : "ASC";
        boolean hasSearch = search != null && !search.isBlank();

        StringBuilder where = new StringBuilder("""
                FROM suppliers
                WHERE owner_id = :ownerId
                  AND deleted_at IS NULL
                """);
        if (active != null) {
            where.append("  AND is_active = :active\n");
        }
        if (hasSearch) {
            where.append("""
                      AND (
                           name ILIKE '%' || :search || '%'
                        OR document_number ILIKE '%' || :search || '%'
                        OR email ILIKE '%' || :search || '%'
                        OR phone ILIKE '%' || :search || '%'
                      )
                    """);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("ownerId", ownerId);
        params.put("size", safeSize);
        params.put("offset", (long) safePage * safeSize);
        if (active != null) {
            params.put("active", active);
        }
        if (hasSearch) {
            params.put("search", search.trim());
        }

        Long total = jdbcClient.sql("SELECT COUNT(*) " + where)
                .params(params)
                .query(Long.class)
                .single();

        List<SupplierResponse> content = jdbcClient.sql("""
                        SELECT id, name, document_type_id, document_number, contact_name,
                               email, phone, address, is_active
                        """ + where + " ORDER BY " + orderColumn + " " + orderDir + " LIMIT :size OFFSET :offset")
                .params(params)
                .query(this::mapSupplierResponse)
                .list();

        return PageResponse.of(content, total == null ? 0 : total, safePage, safeSize);
    }

    @Override
    public Optional<SupplierResponse> findResponseById(UUID supplierId, UUID ownerId) {
        return jdbcClient.sql("""
                        SELECT id, name, document_type_id, document_number, contact_name,
                               email, phone, address, is_active
                        FROM suppliers
                        WHERE id = :supplierId
                          AND owner_id = :ownerId
                          AND deleted_at IS NULL
                        """)
                .param("supplierId", supplierId)
                .param("ownerId", ownerId)
                .query(this::mapSupplierResponse)
                .optional();
    }

    private SupplierResponse mapSupplierResponse(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new SupplierResponse(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getObject("document_type_id", UUID.class),
                rs.getString("document_number"),
                rs.getString("contact_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("address"),
                rs.getBoolean("is_active"));
    }
}
