package com.beet.backend.modules.supplier.infrastructure.output.persistence.jdbc.aggregate;

import com.beet.backend.shared.domain.model.BaseAuditableAggregate;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("suppliers")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierAggregate extends BaseAuditableAggregate {
    @Id
    private UUID id;
    private UUID ownerId;
    private UUID documentTypeId;
    private String documentNumber;
    private String name;
    private String contactName;
    private String email;
    private String phone;
    private String address;
    private Boolean isActive;
}
