package com.beet.backend.modules.supplier.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.supplier.application.dto.SupplierResponse;
import com.beet.backend.modules.supplier.application.port.out.SupplierQueryPort;
import com.beet.backend.modules.supplier.domain.model.SupplierDomain;
import com.beet.backend.modules.supplier.domain.spi.SupplierPersistencePort;
import com.beet.backend.modules.supplier.infrastructure.output.persistence.jdbc.mapper.SupplierAggregateMapper;
import com.beet.backend.modules.supplier.infrastructure.output.persistence.jdbc.repository.SupplierJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SupplierJdbcAdapter implements SupplierPersistencePort, SupplierQueryPort {

    private final SupplierJdbcRepository repository;
    private final SupplierAggregateMapper mapper;
    private final JdbcClient jdbcClient;

    @Override
    public SupplierDomain save(SupplierDomain supplier) {
        var saved = repository.save(mapper.toAggregate(supplier));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<SupplierDomain> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByOwnerAndDocument(UUID ownerId, UUID documentTypeId, String documentNumber) {
        return repository.existsByOwnerIdAndDocumentTypeIdAndDocumentNumber(
                ownerId, documentTypeId, documentNumber);
    }

    @Override
    public List<SupplierResponse> findAllActiveByOwnerId(UUID ownerId) {
        String sql = """
                    SELECT id, name, document_type_id, document_number, contact_name,
                           email, phone, address, is_active
                    FROM suppliers
                    WHERE owner_id = :ownerId
                      AND is_active = true
                    ORDER BY name ASC
                """;

        return jdbcClient.sql(sql)
                .param("ownerId", ownerId)
                .query((rs, rowNum) -> new SupplierResponse(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("name"),
                        rs.getObject("document_type_id", UUID.class),
                        rs.getString("document_number"),
                        rs.getString("contact_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getBoolean("is_active")))
                .list();
    }
}
