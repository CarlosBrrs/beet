package com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.aggregate;

import com.beet.backend.shared.domain.model.BaseAuditableAggregate;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("master_ingredients")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasterIngredientAggregate extends BaseAuditableAggregate {
    @Id
    private UUID id;
    private UUID ownerId;
    private String name;
    private UUID baseUnitId;
    private UUID activeSupplierItemId;
}
