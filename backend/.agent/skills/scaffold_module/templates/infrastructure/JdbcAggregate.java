package com.beet.backend.modules.{moduleName}.infrastructure.output.persistence.jdbc.aggregate;

import com.beet.backend.shared.infrastructure.output.persistence.jdbc.aggregate.BaseAuditableAggregate;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("{table_name}")@Data @EqualsAndHashCode(callSuper=true)@Builder @AllArgsConstructor @NoArgsConstructor public class{Aggregate}Aggregate extends BaseAuditableAggregate{@Id private UUID id;private String name;
// Add other table columns here
}
