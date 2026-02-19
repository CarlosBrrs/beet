package com.beet.backend.modules.user.infrastructure.output.persistence.jdbc.aggregate;

import com.beet.backend.shared.domain.model.BaseAuditableAggregate;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("users")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAggregate extends BaseAuditableAggregate {
    @Id
    private UUID id;

    private String email;
    private String passwordHash;
    private String firstName;
    private String secondName;
    private String firstLastname;
    private String secondLastname;
    private String phoneNumber;
    private String username;

    // Foreign Keys
    private UUID ownerId;
    private UUID subscriptionPlanId;
}
