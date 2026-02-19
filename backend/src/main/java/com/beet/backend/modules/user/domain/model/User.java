package com.beet.backend.modules.user.domain.model;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
public class User {
    private final UUID id;
    private String email;
    private String password; // Raw password (transient)
    private String passwordHash;
    private String firstName;
    private String secondName;
    private String firstLastname;
    private String secondLastname;
    private String phoneNumber;
    private String username;

    // Self-reference to Owner
    private UUID ownerId;

    // Reference to Plan
    private UUID subscriptionPlanId;

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null)
            sb.append(firstName);
        if (secondName != null)
            sb.append(" ").append(secondName);
        if (firstLastname != null)
            sb.append(" ").append(firstLastname);
        if (secondLastname != null)
            sb.append(" ").append(secondLastname);
        return sb.toString().trim();
    }
}
