package com.beet.backend.modules.unit.domain.spi;

import com.beet.backend.modules.unit.domain.model.UnitDomain;

import java.util.List;

/**
 * Persistence port for read-only access to units with their conversion factors.
 */
public interface UnitPersistencePort {

    /** Returns all units with their factorToBase already resolved. */
    List<UnitDomain> findAllWithConversions();
}
