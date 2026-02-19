package com.beet.backend.modules.user.infrastructure.output.persistence.jdbc.mapper;

import com.beet.backend.modules.user.domain.model.User;
import com.beet.backend.modules.user.infrastructure.output.persistence.jdbc.aggregate.UserAggregate;
import org.springframework.stereotype.Component;

@Component
public class UserAggregateMapper {

    public UserAggregate toAggregate(User user) {
        if (user == null)
            return null;

        return UserAggregate.builder()
                .id(user.getId())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .firstName(user.getFirstName())
                .secondName(user.getSecondName())
                .firstLastname(user.getFirstLastname())
                .secondLastname(user.getSecondLastname())
                .phoneNumber(user.getPhoneNumber())
                .username(user.getUsername())
                .ownerId(user.getOwnerId())
                .subscriptionPlanId(user.getSubscriptionPlanId())
                .build();
    }

    public User toDomain(UserAggregate aggregate) {
        if (aggregate == null)
            return null;

        return User.builder()
                .id(aggregate.getId())
                .email(aggregate.getEmail())
                .passwordHash(aggregate.getPasswordHash())
                .firstName(aggregate.getFirstName())
                .secondName(aggregate.getSecondName())
                .firstLastname(aggregate.getFirstLastname())
                .secondLastname(aggregate.getSecondLastname())
                .phoneNumber(aggregate.getPhoneNumber())
                .username(aggregate.getUsername())
                .ownerId(aggregate.getOwnerId())
                .subscriptionPlanId(aggregate.getSubscriptionPlanId())
                .build();
    }
}
