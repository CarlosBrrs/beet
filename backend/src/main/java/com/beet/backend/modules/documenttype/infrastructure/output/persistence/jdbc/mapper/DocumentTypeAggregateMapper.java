package com.beet.backend.modules.documenttype.infrastructure.output.persistence.jdbc.mapper;

import com.beet.backend.modules.documenttype.domain.model.DocumentTypeDomain;
import com.beet.backend.modules.documenttype.infrastructure.output.persistence.jdbc.aggregate.DocumentTypeAggregate;
import org.springframework.stereotype.Component;

@Component
public class DocumentTypeAggregateMapper {

    public DocumentTypeDomain toDomain(DocumentTypeAggregate aggregate) {
        return DocumentTypeDomain.builder()
                .id(aggregate.getId())
                .name(aggregate.getName())
                .description(aggregate.getDescription())
                .build();
    }
}
