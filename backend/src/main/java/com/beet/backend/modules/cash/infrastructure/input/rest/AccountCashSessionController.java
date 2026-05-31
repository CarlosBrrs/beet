package com.beet.backend.modules.cash.infrastructure.input.rest;

import com.beet.backend.modules.cash.application.dto.CashSessionListResponse;
import com.beet.backend.modules.cash.application.handler.CashHandler;
import com.beet.backend.modules.cash.domain.model.CashSessionStatus;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/account/cash-sessions")
@RequiredArgsConstructor
public class AccountCashSessionController {

    private final CashHandler handler;

    @GetMapping
    public ResponseEntity<ApiGenericResponse<PageResponse<CashSessionListResponse>>> list(
            @RequestParam(required = false) UUID restaurantId,
            @RequestParam(required = false) CashSessionStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "UTC") String timeZone,
            @RequestParam(required = false) UUID cashRegisterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(handler.listAccountSessions(
                restaurantId, status, from, to, timeZone, cashRegisterId, page, size));
    }
}
