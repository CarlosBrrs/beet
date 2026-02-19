package com.beet.backend.modules.unit.application.handler;

import com.beet.backend.modules.unit.application.dto.UnitResponse;
import com.beet.backend.modules.unit.application.mapper.UnitServiceMapper;
import com.beet.backend.modules.unit.domain.api.UnitServicePort;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UnitHandlerImpl implements UnitHandler {

    private final UnitServicePort servicePort;
    private final UnitServiceMapper mapper;

    @Override
    public ApiGenericResponse<List<UnitResponse>> getAllUnits() {
        List<UnitResponse> response = servicePort.getAllUnits().stream()
                .map(mapper::toResponse)
                .toList();
        return ApiGenericResponse.success(response);
    }
}
