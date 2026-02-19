package com.beet.backend.modules.unit.application.handler;

import com.beet.backend.modules.unit.application.dto.UnitResponse;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;

import java.util.List;

public interface UnitHandler {

    ApiGenericResponse<List<UnitResponse>> getAllUnits();
}
