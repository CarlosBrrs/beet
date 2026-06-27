package com.beet.backend.modules.staff.domain.api;

import com.beet.backend.modules.staff.application.dto.*;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.util.List;
import java.util.UUID;

public interface StaffServicePort {
    List<PermissionCatalogResponse> permissionCatalog();

    List<StaffRoleResponse> listRoles(UUID restaurantId);

    PageResponse<StaffMemberResponse> listRestaurantStaff(UUID restaurantId, int page, int size, String search, String status, UUID roleId);

    StaffMemberResponse getRestaurantStaff(UUID restaurantId, UUID userId);

    StaffMemberResponse updateRestaurantStaff(UUID restaurantId, UUID userId, StaffAssignmentRequest request, UUID actorId);

    void removeRestaurantStaff(UUID restaurantId, UUID userId, UUID actorId);

    PageResponse<StaffInvitationResponse> listInvitations(UUID restaurantId, int page, int size, String search, String status);

    StaffInvitationResponse createInvitation(UUID restaurantId, StaffInvitationRequest request, UUID actorId);

    StaffInvitationResponse regenerateInvitation(UUID restaurantId, UUID invitationId, UUID actorId);

    StaffInvitationResponse revokeInvitation(UUID restaurantId, UUID invitationId, UUID actorId, String reason);

    StaffInvitationResponse previewInvitation(String token);

    StaffMemberResponse acceptInvitation(String token, AcceptStaffInvitationRequest request);

    PageResponse<StaffMemberResponse> listAccountStaff(UUID ownerId, int page, int size, String search, String status, UUID restaurantId);

    StaffMemberResponse getAccountStaff(UUID ownerId, UUID userId);

    StaffMemberResponse updateAccountStatus(UUID ownerId, UUID userId, AccountStatusRequest request);
}
