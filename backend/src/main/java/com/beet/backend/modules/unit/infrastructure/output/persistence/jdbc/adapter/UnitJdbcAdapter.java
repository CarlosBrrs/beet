package com.beet.backend.modules.unit.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.unit.domain.model.UnitDomain;
import com.beet.backend.modules.unit.domain.spi.UnitPersistencePort;
import com.beet.backend.modules.unit.infrastructure.output.persistence.jdbc.mapper.UnitAggregateMapper;
import com.beet.backend.modules.unit.infrastructure.output.persistence.jdbc.repository.UnitJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UnitJdbcAdapter implements UnitPersistencePort {

    private final UnitJdbcRepository repository;
    private final UnitAggregateMapper mapper;

    @Override
    public List<UnitDomain> findAllWithConversions() {
        return repository.findAllWithConversions().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
