package com.beet.backend.modules.restaurant.infrastructure.output.persistence.jdbc.aggregate;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import com.beet.backend.shared.domain.model.BaseAuditableAggregate;
import com.beet.backend.shared.domain.model.OperationMode;
import com.beet.backend.modules.restaurant.domain.model.RestaurantSettings;

import java.util.UUID;

@Table("restaurants")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantAggregate extends BaseAuditableAggregate {
    @Id
    private UUID id;
    private String name;
    private String address;
    private String email;
    private String phoneNumber;
    private OperationMode operationMode;
    private RestaurantSettings settings;
    private Boolean isActive;
    private UUID ownerId;
}
