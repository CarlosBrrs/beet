package com.beet.backend.modules.unit.infrastructure.output.persistence.jdbc.aggregate;

import com.beet.backend.modules.unit.domain.model.UnitType;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Projection for the units + unit_conversions JOIN result.
 *
 * NOTE: unit_conversions has a composite PK (from_unit_id, to_unit_id),
 * which Spring Data JDBC CrudRepository does not support natively —
 * it requires a single @Id field. That is why we use a @Query with
 * a custom RowMapper and JOIN instead of a separate CrudRepository.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnitConversionProjection {
    private UUID id;
    private String name;
    private String abbreviation;
    private UnitType type;
    private Boolean isBase;
    private BigDecimal factorToBase;
}
