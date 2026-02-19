package com.beet.backend.modules.unit.domain.api;

import com.beet.backend.modules.unit.domain.model.UnitDomain;

import java.util.List;

/**
 * Service port for read-only access to units.
 */
public interface UnitServicePort {

    /** Returns all units with their factorToBase resolved. */
List<UnitDomain> getAllUnits();
}
