package com.beet.backend.modules.supplier.infrastructure.input.rest;

import com.beet.backend.modules.supplier.application.dto.SupplierResponse;
import com.beet.backend.modules.supplier.application.handler.SupplierHandler;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierHandler handler;

    @GetMapping
    public ResponseEntity<ApiGenericResponse<List<SupplierResponse>>> findAllActive() {
        UUID ownerId = SecurityUtils.getEffectiveOwnerId();
        return ResponseEntity.ok(handler.findAllActive(ownerId));
    }
}
