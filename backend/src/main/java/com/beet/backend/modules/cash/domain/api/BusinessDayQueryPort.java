package com.beet.backend.modules.cash.domain.api;

import com.beet.backend.modules.cash.domain.model.RestaurantBusinessDayDomain;

import java.util.Optional;
import java.util.UUID;

public interface BusinessDayQueryPort {
    Optional<RestaurantBusinessDayDomain> findOpenBusinessDay(UUID restaurantId);

    RestaurantBusinessDayDomain requireOpenBusinessDay(UUID restaurantId);
}
