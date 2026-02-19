package com.beet.backend.modules.restaurant.domain.model;

import lombok.*;
import com.beet.backend.shared.domain.model.OperationMode;

import java.util.UUID;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
public class RestaurantDomain {
    private final UUID id;
    private String name;
    private String address;
    private String email;
    private String phoneNumber;
    private OperationMode operationMode;
    private Boolean isActive;
    private UUID ownerId;
    private RestaurantSettings settings;
}
