package com.beet.backend.modules.cash.domain.spi;

import com.beet.backend.modules.cash.domain.model.CashRegisterDomain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashRegisterPersistencePort {
    CashRegisterDomain save(CashRegisterDomain register);

    CashRegisterDomain update(CashRegisterDomain register);

    Optional<CashRegisterDomain> findRegisterById(UUID id);

    Optional<CashRegisterDomain> findRegisterByIdForUpdate(UUID id);

    List<CashRegisterDomain> findByRestaurantId(UUID restaurantId);

    boolean existsByName(UUID restaurantId, String name);

}
