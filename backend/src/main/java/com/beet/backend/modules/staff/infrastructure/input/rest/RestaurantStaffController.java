package com.beet.backend.modules.staff.infrastructure.input.rest;

import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import com.beet.backend.modules.staff.application.dto.*;
import com.beet.backend.modules.staff.domain.api.StaffServicePort;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.security.RequiresPermission;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/restaurants/{restaurantId}/staff")
@RequiredArgsConstructor
public class RestaurantStaffController {
    private final StaffServicePort staffService;

    @GetMapping("/permission-catalog")
    @RequiresPermission(module = PermissionModule.STAFF, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<List<PermissionCatalogResponse>>> permissionCatalog(
            @PathVariable UUID restaurantId) {
        return ResponseEntity.ok(ApiGenericResponse.success(staffService.permissionCatalog()));
    }

    @GetMapping("/roles")
    @RequiresPermission(module = PermissionModule.STAFF, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<List<StaffRoleResponse>>> listRoles(@PathVariable UUID restaurantId) {
        return ResponseEntity.ok(ApiGenericResponse.success(staffService.listRoles(restaurantId)));
    }
    @GetMapping
    @RequiresPermission(module = PermissionModule.STAFF, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<PageResponse<StaffMemberResponse>>> listStaff(
            @PathVariable UUID restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID roleId) {
        return ResponseEntity.ok(ApiGenericResponse.success(
                staffService.listRestaurantStaff(restaurantId, page, size, search, status, roleId)));
    }

    @GetMapping("/{userId}")
    @RequiresPermission(module = PermissionModule.STAFF, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<StaffMemberResponse>> getStaff(
            @PathVariable UUID restaurantId,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiGenericResponse.success(staffService.getRestaurantStaff(restaurantId, userId)));
    }

    @PatchMapping("/{userId}")
    @RequiresPermission(module = PermissionModule.STAFF, action = PermissionAction.EDIT)
    public ResponseEntity<ApiGenericResponse<StaffMemberResponse>> updateStaff(
            @PathVariable UUID restaurantId,
            @PathVariable UUID userId,
            @Valid @RequestBody StaffAssignmentRequest request) {
        return ResponseEntity.ok(ApiGenericResponse.success(
                staffService.updateRestaurantStaff(restaurantId, userId, request, SecurityUtils.getAuthenticatedUserId())));
    }

    @DeleteMapping("/{userId}")
    @RequiresPermission(module = PermissionModule.STAFF, action = PermissionAction.DELETE)
    public ResponseEntity<ApiGenericResponse<Void>> removeStaff(
            @PathVariable UUID restaurantId,
            @PathVariable UUID userId) {
        staffService.removeRestaurantStaff(restaurantId, userId, SecurityUtils.getAuthenticatedUserId());
        return ResponseEntity.ok(ApiGenericResponse.success(null));
    }

    @GetMapping("/invitations")
    @RequiresPermission(module = PermissionModule.STAFF, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<PageResponse<StaffInvitationResponse>>> listInvitations(
            @PathVariable UUID restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiGenericResponse.success(
                staffService.listInvitations(restaurantId, page, size, search, status)));
    }

    @PostMapping("/invitations")
    @RequiresPermission(module = PermissionModule.STAFF, action = PermissionAction.CREATE)
    public ResponseEntity<ApiGenericResponse<StaffInvitationResponse>> createInvitation(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody StaffInvitationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiGenericResponse.success(
                staffService.createInvitation(restaurantId, request, SecurityUtils.getAuthenticatedUserId())));
    }

    @PostMapping("/invitations/{invitationId}/regenerate")
    @RequiresPermission(module = PermissionModule.STAFF, action = PermissionAction.CREATE)
    public ResponseEntity<ApiGenericResponse<StaffInvitationResponse>> regenerateInvitation(
            @PathVariable UUID restaurantId,
            @PathVariable UUID invitationId) {
        return ResponseEntity.ok(ApiGenericResponse.success(
                staffService.regenerateInvitation(restaurantId, invitationId, SecurityUtils.getAuthenticatedUserId())));
    }

    @PostMapping("/invitations/{invitationId}/revoke")
    @RequiresPermission(module = PermissionModule.STAFF, action = PermissionAction.DELETE)
    public ResponseEntity<ApiGenericResponse<StaffInvitationResponse>> revokeInvitation(
            @PathVariable UUID restaurantId,
            @PathVariable UUID invitationId,
            @RequestBody(required = false) StaffRevokeInvitationRequest request) {
        return ResponseEntity.ok(ApiGenericResponse.success(
                staffService.revokeInvitation(restaurantId, invitationId, SecurityUtils.getAuthenticatedUserId(),
                        request == null ? null : request.reason())));
    }
}

