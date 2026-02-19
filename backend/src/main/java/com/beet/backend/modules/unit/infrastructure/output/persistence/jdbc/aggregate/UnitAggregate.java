package com.beet.backend.modules.unit.infrastructure.output.persistence.jdbc.aggregate;

import com.beet.backend.modules.unit.domain.model.UnitType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("units")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UnitAggregate {
    @Id
    private UUID id;
    private String name;
    private String abbreviation;
    private UnitType type;
    private Boolean isBase;
}
