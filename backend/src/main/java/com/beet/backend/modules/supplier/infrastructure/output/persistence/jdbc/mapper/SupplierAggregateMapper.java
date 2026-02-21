package com.beet.backend.modules.supplier.infrastructure.output.persistence.jdbc.mapper;

import com.beet.backend.modules.supplier.domain.model.SupplierDomain;
import com.beet.backend.modules.supplier.infrastructure.output.persistence.jdbc.aggregate.SupplierAggregate;
import org.springframework.stereotype.Component;

@Component
public class SupplierAggregateMapper {

    public SupplierDomain toDomain(SupplierAggregate aggregate) {
        if (aggregate == null)
            return null;
        return SupplierDomain.builder()
                .id(aggregate.getId())
                .ownerId(aggregate.getOwnerId())
                .documentTypeId(aggregate.getDocumentTypeId())
                .documentNumber(aggregate.getDocumentNumber())
                .name(aggregate.getName())
                .contactName(aggregate.getContactName())
                .email(aggregate.getEmail())
                .phone(aggregate.getPhone())
                .address(aggregate.getAddress())
                .isActive(aggregate.getIsActive())
                .build();
    }

    public SupplierAggregate toAggregate(SupplierDomain domain) {
        if (domain == null)
            return null;
        return SupplierAggregate.builder()
                .id(domain.getId())
                .ownerId(domain.getOwnerId())
                .documentTypeId(domain.getDocumentTypeId())
                .documentNumber(domain.getDocumentNumber())
                .name(domain.getName())
                .contactName(domain.getContactName())
                .email(domain.getEmail())
                .phone(domain.getPhone())
                .address(domain.getAddress())
                .isActive(domain.getIsActive())
                .build();
    }
}
