package com.beet.backend.modules.cash.domain.api;

import com.beet.backend.modules.cash.domain.model.CashRegisterDomain;

import java.util.List;
import java.util.UUID;

public interface CashRegisterServicePort {
    CashRegisterDomain create(CashRegisterDomain register);

    CashRegisterDomain update(CashRegisterDomain register);

    CashRegisterDomain deactivate(UUID restaurantId, UUID registerId, UUID updatedBy);

    CashRegisterDomain getById(UUID registerId);

    List<CashRegisterDomain> listByRestaurant(UUID restaurantId);
}
