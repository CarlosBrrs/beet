package com.beet.backend.modules.staff.infrastructure.input.rest;

import com.beet.backend.modules.staff.application.dto.AccountStatusRequest;
import com.beet.backend.modules.staff.application.dto.StaffMemberResponse;
import com.beet.backend.modules.staff.domain.api.StaffServicePort;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/account/staff")
@RequiredArgsConstructor
public class AccountStaffController {
    private final StaffServicePort staffService;

    @GetMapping
    public ResponseEntity<ApiGenericResponse<PageResponse<StaffMemberResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID restaurantId) {
        return ResponseEntity.ok(ApiGenericResponse.success(
                staffService.listAccountStaff(SecurityUtils.getAuthenticatedUserId(), page, size, search, status, restaurantId)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiGenericResponse<StaffMemberResponse>> detail(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiGenericResponse.success(
                staffService.getAccountStaff(SecurityUtils.getAuthenticatedUserId(), userId)));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiGenericResponse<StaffMemberResponse>> updateStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody AccountStatusRequest request) {
        return ResponseEntity.ok(ApiGenericResponse.success(
                staffService.updateAccountStatus(SecurityUtils.getAuthenticatedUserId(), userId, request)));
    }
}
