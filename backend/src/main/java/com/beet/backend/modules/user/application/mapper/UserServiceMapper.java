package com.beet.backend.modules.user.application.mapper;

import com.beet.backend.modules.user.application.dto.UserResponse;
import com.beet.backend.modules.user.domain.model.User;
import com.beet.backend.modules.user.application.dto.RegisterUserRequest;
import org.springframework.stereotype.Component;

@Component
public class UserServiceMapper {

    public UserResponse toResponse(User user) {
        if (user == null)
            return null;

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .subscriptionPlanId(user.getSubscriptionPlanId())
                .build();
    }

    public User toDomain(RegisterUserRequest request) {
        if (request == null)
            return null;

        return User.builder()
                .email(request.email())
                .password(request.password())
                .firstName(request.firstName())
                .secondName(request.secondName())
                .firstLastname(request.firstLastname())
                .secondLastname(request.secondLastname())
                .phoneNumber(request.phoneNumber())
                .username(request.username())
                .subscriptionPlanId(request.subscriptionPlanId())
                .build();
    }
}
