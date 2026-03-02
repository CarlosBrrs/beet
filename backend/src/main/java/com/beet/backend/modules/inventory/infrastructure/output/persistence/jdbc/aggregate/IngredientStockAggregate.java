package com.beet.backend.modules.inventory.infrastructure.output.persistence.jdbc.aggregate;

import com.beet.backend.shared.domain.model.BaseAuditableAggregate;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Table("ingredient_stocks")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientStockAggregate extends BaseAuditableAggregate {
    @Id
    private UUID id; // null on insert → DB generates via gen_random_uuid()
    private UUID masterIngredientId;
    private UUID restaurantId;
    private BigDecimal currentStock;
    private BigDecimal minStock;
}
