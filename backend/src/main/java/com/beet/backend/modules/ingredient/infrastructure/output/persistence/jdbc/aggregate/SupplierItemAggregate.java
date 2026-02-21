package com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.aggregate;

import com.beet.backend.shared.domain.model.BaseAuditableAggregate;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Table("supplier_items")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierItemAggregate extends BaseAuditableAggregate {
    @Id
    private UUID id;
    private UUID masterIngredientId;
    private UUID supplierId;
    private String brandName;
    private String purchaseUnitName;
    private BigDecimal conversionFactor;
    private BigDecimal lastCostBase;
}
