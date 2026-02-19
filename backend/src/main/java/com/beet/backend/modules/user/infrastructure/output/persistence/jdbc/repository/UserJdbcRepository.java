package com.beet.backend.modules.user.infrastructure.output.persistence.jdbc.repository;

import com.beet.backend.modules.user.infrastructure.output.persistence.jdbc.aggregate.UserAggregate;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserJdbcRepository extends CrudRepository<UserAggregate, UUID> {

    Optional<UserAggregate> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    // TODO remover luego de implementar el servicio de suscripciones
    // Check if a subscription plan exists.
    // Since we don't have a specific SubscriptionAggregate yet in this module,
    // we can use a raw query check against the 'subscription_plans' table.
    @Query("SELECT COUNT(*) > 0 FROM subscription_plans WHERE id = :id")
    boolean existsSubscriptionPlan(@Param("id") UUID id);

}
