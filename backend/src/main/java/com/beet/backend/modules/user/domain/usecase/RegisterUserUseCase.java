package com.beet.backend.modules.user.domain.usecase;

import com.beet.backend.modules.user.domain.api.RegisterUserServicePort;
import com.beet.backend.modules.user.domain.exception.UserAlreadyExistsException;
import com.beet.backend.modules.user.domain.model.User;
import com.beet.backend.modules.user.domain.spi.UserPersistencePort;
import com.beet.backend.modules.subscription.domain.exception.SubscriptionPlanNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase implements RegisterUserServicePort {

    private final UserPersistencePort userPersistencePort;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public User register(User user) {
        // 1. Check Duplicates
        if (userPersistencePort.existsByEmail(user.getEmail())) {
            throw UserAlreadyExistsException.forEmail(user.getEmail());
        }
        if (userPersistencePort.existsByUsername(user.getUsername())) {
            throw UserAlreadyExistsException.forUsername(user.getUsername());
        }
        if (userPersistencePort.existsByPhoneNumber(user.getPhoneNumber())) {
            throw UserAlreadyExistsException.forPhoneNumber(user.getPhoneNumber());
        }

        // TODO: 2. Validate Plan, this needs to come from subscription plan service
        if (!userPersistencePort.existsSubscriptionPlan(user.getSubscriptionPlanId())) {
            throw SubscriptionPlanNotFoundException.forId(user.getSubscriptionPlanId());
        }

        // 3. Hash Password
        String encodedPassword = passwordEncoder.encode(user.getPassword());

        // 4. Create Domain Object
        // Database generates UUID. OwnerId is null (signifying root owner).

        User newUser = User.builder()
                .id(null)
                .email(user.getEmail())
                .passwordHash(encodedPassword)
                .firstName(user.getFirstName())
                .secondName(user.getSecondName())
                .firstLastname(user.getFirstLastname())
                .secondLastname(user.getSecondLastname())
                .phoneNumber(user.getPhoneNumber())
                .username(user.getUsername())
                .subscriptionPlanId(user.getSubscriptionPlanId())
                .ownerId(null) // Null signifies they ARE the owner (root)
                .build();

        // 5. Save
        return userPersistencePort.save(newUser);
    }
}
