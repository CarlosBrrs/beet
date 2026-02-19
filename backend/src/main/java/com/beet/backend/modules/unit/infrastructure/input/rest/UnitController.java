package com.beet.backend.modules.unit.infrastructure.input.rest;

import com.beet.backend.modules.unit.application.dto.UnitResponse;
import com.beet.backend.modules.unit.application.handler.UnitHandler;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitHandler handler;

    /**
     * GET /units — returns all units with their factorToBase.
     */
    @GetMapping
    public ResponseEntity<ApiGenericResponse<List<UnitResponse>>> getUnits() {
        return ResponseEntity.ok(handler.getAllUnits());
    }
}
