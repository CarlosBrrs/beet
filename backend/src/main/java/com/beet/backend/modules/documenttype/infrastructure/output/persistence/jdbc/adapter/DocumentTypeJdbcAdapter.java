package com.beet.backend.modules.documenttype.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.documenttype.domain.model.DocumentTypeDomain;
import com.beet.backend.modules.documenttype.domain.spi.DocumentTypePersistencePort;
import com.beet.backend.modules.documenttype.infrastructure.output.persistence.jdbc.mapper.DocumentTypeAggregateMapper;
import com.beet.backend.modules.documenttype.infrastructure.output.persistence.jdbc.repository.DocumentTypeJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DocumentTypeJdbcAdapter implements DocumentTypePersistencePort {

    private final DocumentTypeJdbcRepository repository;
    private final DocumentTypeAggregateMapper mapper;

    @Override
    public List<DocumentTypeDomain> findByCountryCode(String countryCode) {
        return repository.findByCountryCode(countryCode).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
