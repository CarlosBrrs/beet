package com.beet.backend.modules.staff.infrastructure.input.rest;

import com.beet.backend.modules.staff.application.dto.AcceptStaffInvitationRequest;
import com.beet.backend.modules.staff.application.dto.StaffInvitationResponse;
import com.beet.backend.modules.staff.application.dto.StaffMemberResponse;
import com.beet.backend.modules.staff.domain.api.StaffServicePort;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/staff-invitations")
@RequiredArgsConstructor
public class PublicStaffInvitationController {
    private final StaffServicePort staffService;

    @GetMapping("/{token}")
    public ResponseEntity<ApiGenericResponse<StaffInvitationResponse>> preview(@PathVariable String token) {
        return ResponseEntity.ok(ApiGenericResponse.success(staffService.previewInvitation(token)));
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<ApiGenericResponse<StaffMemberResponse>> accept(
            @PathVariable String token,
            @Valid @RequestBody AcceptStaffInvitationRequest request) {
        return ResponseEntity.ok(ApiGenericResponse.success(staffService.acceptInvitation(token, request)));
    }
}
