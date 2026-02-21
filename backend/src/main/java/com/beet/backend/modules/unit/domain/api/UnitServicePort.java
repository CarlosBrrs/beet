package com.beet.backend.modules.unit.domain.api;

import com.beet.backend.modules.unit.domain.model.UnitDomain;

import java.util.List;
import java.util.UUID;

/**
 * Service port for read-only access to units.
 */
public interface UnitServicePort {

    /** Returns all units with their factorToBase resolved. */
    List<UnitDomain> getAllUnits();

    /** Returns a single unit by id with its factorToBase resolved. */
    UnitDomain findById(UUID id);
}
