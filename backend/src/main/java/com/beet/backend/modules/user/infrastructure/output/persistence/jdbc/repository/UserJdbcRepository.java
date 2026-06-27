package com.beet.backend.modules.user.infrastructure.output.persistence.jdbc.repository;

import com.beet.backend.modules.user.infrastructure.output.persistence.jdbc.aggregate.UserAggregate;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserJdbcRepository extends CrudRepository<UserAggregate, UUID> {

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) AND deleted_at IS NULL")
    Optional<UserAggregate> findByEmail(String email);

    @Query("SELECT COUNT(*) > 0 FROM users WHERE LOWER(email) = LOWER(:email) AND deleted_at IS NULL")
    boolean existsByEmail(String email);

    @Query("SELECT COUNT(*) > 0 FROM users WHERE username = :username AND deleted_at IS NULL")
    boolean existsByUsername(String username);

    @Query("SELECT COUNT(*) > 0 FROM users WHERE phone_number = :phoneNumber AND deleted_at IS NULL")
    boolean existsByPhoneNumber(String phoneNumber);

    // TODO remover luego de implementar el servicio de suscripciones
    // Check if a subscription plan exists.
    // Since we don't have a specific SubscriptionAggregate yet in this module,
    // we can use a raw query check against the 'subscription_plans' table.
    @Query("SELECT COUNT(*) > 0 FROM subscription_plans WHERE id = :id")
    boolean existsSubscriptionPlan(@Param("id") UUID id);

}
