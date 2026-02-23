package com.beet.backend.modules.documenttype.infrastructure.output.persistence.jdbc.aggregate;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Data;
import java.util.UUID;

@Data
@Table("document_types")
public class DocumentTypeAggregate {
    @Id
    private UUID id;
    private String name;
    private String description;
    private UUID countryId;
}
