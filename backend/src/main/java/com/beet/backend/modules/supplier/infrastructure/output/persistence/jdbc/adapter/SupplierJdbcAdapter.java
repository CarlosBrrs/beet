package com.beet.backend.modules.supplier.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.supplier.domain.model.SupplierDomain;
import com.beet.backend.modules.supplier.domain.spi.SupplierPersistencePort;
import com.beet.backend.modules.supplier.infrastructure.output.persistence.jdbc.mapper.SupplierAggregateMapper;
import com.beet.backend.modules.supplier.infrastructure.output.persistence.jdbc.repository.SupplierJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SupplierJdbcAdapter implements SupplierPersistencePort {

    private final SupplierJdbcRepository repository;
    private final SupplierAggregateMapper mapper;

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
        return repository.existsByOwnerIdAndDocumentTypeIdAndDocumentNumber(ownerId, documentTypeId, documentNumber);
    }
}
