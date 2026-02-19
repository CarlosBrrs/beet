package com.beet.backend.modules.subscription.infrastructure.output.persistence.jdbc.repository;

import com.beet.backend.modules.subscription.infrastructure.output.persistence.jdbc.aggregate.SubscriptionPlanAggregate;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SubscriptionPlanJdbcRepository extends CrudRepository<SubscriptionPlanAggregate, UUID> {
}
