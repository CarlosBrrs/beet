package com.beet.backend.modules.staff.domain.usecase;

import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import com.beet.backend.modules.staff.application.dto.*;
import com.beet.backend.modules.staff.domain.api.StaffServicePort;
import com.beet.backend.modules.user.domain.model.User;
import com.beet.backend.modules.user.domain.model.UserAccountStatus;
import com.beet.backend.modules.user.domain.spi.UserPersistencePort;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffUseCase implements StaffServicePort {
    private static final int INVITATION_DAYS = 7;
    private static final int MAX_PAGE_SIZE = 100;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<PermissionModule, List<PermissionAction>>> PERMISSIONS_TYPE =
            new TypeReference<>() {};

    private final JdbcClient jdbc;
    private final UserPersistencePort userPersistence;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<PermissionCatalogResponse> permissionCatalog() {
        return List.of(
                catalog(PermissionModule.STAFF, "Staff", PermissionAction.VIEW, PermissionAction.CREATE,
                        PermissionAction.EDIT, PermissionAction.DELETE, PermissionAction.MANAGE),
                catalog(PermissionModule.RESTAURANTS, "Restaurant settings", PermissionAction.VIEW,
                        PermissionAction.EDIT, PermissionAction.MANAGE),
                catalog(PermissionModule.TABLES, "Tables", PermissionAction.VIEW, PermissionAction.CREATE,
                        PermissionAction.EDIT, PermissionAction.DELETE, PermissionAction.MANAGE),
                catalog(PermissionModule.ORDERS, "Orders", PermissionAction.VIEW, PermissionAction.CREATE,
                        PermissionAction.EDIT, PermissionAction.CANCEL, PermissionAction.COMPLETE,
                        PermissionAction.MANAGE),
                catalog(PermissionModule.KDS, "Kitchen display", PermissionAction.VIEW,
                        PermissionAction.UPDATE_STATUS, PermissionAction.MANAGE),
                catalog(PermissionModule.CASH, "Cash", PermissionAction.VIEW, PermissionAction.OPEN,
                        PermissionAction.CLOSE, PermissionAction.PROCESS, PermissionAction.VOID,
                        PermissionAction.MANAGE),
                catalog(PermissionModule.PAYMENTS, "Payments", PermissionAction.VIEW,
                        PermissionAction.PROCESS, PermissionAction.REFUND, PermissionAction.VOID,
                        PermissionAction.MANAGE),
                catalog(PermissionModule.INVENTORY, "Inventory", PermissionAction.VIEW,
                        PermissionAction.ACTIVATE, PermissionAction.EDIT, PermissionAction.MANAGE),
                catalog(PermissionModule.INVOICES, "Purchases", PermissionAction.VIEW,
                        PermissionAction.CREATE, PermissionAction.EDIT, PermissionAction.DELETE,
                        PermissionAction.MANAGE),
                catalog(PermissionModule.MENUS, "Menus", PermissionAction.VIEW, PermissionAction.CREATE,
                        PermissionAction.EDIT, PermissionAction.DELETE, PermissionAction.MANAGE),
                catalog(PermissionModule.PRODUCTS, "Products", PermissionAction.VIEW,
                        PermissionAction.CREATE, PermissionAction.EDIT, PermissionAction.MANAGE),
                catalog(PermissionModule.PREPARATIONS, "Preparations", PermissionAction.VIEW,
                        PermissionAction.CREATE, PermissionAction.EDIT, PermissionAction.DELETE,
                        PermissionAction.MANAGE),
                catalog(PermissionModule.TEMPLATES, "Templates", PermissionAction.VIEW,
                        PermissionAction.CREATE, PermissionAction.EDIT, PermissionAction.MANAGE),
                catalog(PermissionModule.FINANCE, "Finance reports", PermissionAction.VIEW));
    }

    @Override
    public List<StaffRoleResponse> listRoles(UUID restaurantId) {
        restaurantOwner(restaurantId);
        return jdbc.sql("""
                SELECT r.id, r.name, r.permissions::text, r.preset_key, r.is_active,
                       COUNT(urr.id) FILTER (
                           WHERE urr.restaurant_id = :restaurantId
                             AND urr.assignment_status <> 'REMOVED'
                             AND urr.deleted_at IS NULL
                       ) assigned_users
                FROM roles r
                LEFT JOIN user_restaurant_roles urr ON urr.role_id = r.id
                WHERE r.restaurant_id IS NULL
                  AND r.is_assignable = TRUE
                  AND r.is_active = TRUE
                  AND r.deleted_at IS NULL
                GROUP BY r.id, r.name, r.permissions, r.preset_key, r.is_active
                ORDER BY CASE r.preset_key
                    WHEN 'MANAGER' THEN 10
                    WHEN 'CASHIER' THEN 20
                    WHEN 'WAITER' THEN 30
                    WHEN 'KITCHEN' THEN 40
                    ELSE 100
                END, r.name
                """)
                .param("restaurantId", restaurantId)
                .query((rs, rowNum) -> roleResponse(
                        uuid(rs, "id"),
                        rs.getString("name"),
                        rs.getString("permissions"),
                        rs.getString("preset_key"),
                        rs.getBoolean("is_active"),
                        rs.getLong("assigned_users")))
                .list();
    }
    @Override
    public PageResponse<StaffMemberResponse> listRestaurantStaff(
            UUID restaurantId, int page, int size, String search, String status, UUID roleId) {
        page = Math.max(page, 0);
        size = normalizeSize(size);
        Map<String, Object> params = new HashMap<>();
        params.put("restaurantId", restaurantId);
        params.put("limit", size);
        params.put("offset", page * size);
        params.put("status", status);
        params.put("roleId", roleId);
        String searchSql = "";
        if (search != null && !search.isBlank()) {
            params.put("search", "%" + search.trim().toLowerCase() + "%");
            searchSql = " AND (LOWER(u.email) LIKE :search OR LOWER(u.first_name || ' ' || u.first_lastname) LIKE :search) ";
        }
        String statusSql = status == null || status.isBlank() ? "" : " AND urr.assignment_status = CAST(:status AS staff_assignment_status) ";
        String roleSql = roleId == null ? "" : " AND urr.role_id = :roleId ";
        String from = """
                FROM user_restaurant_roles urr
                JOIN users u ON u.id = urr.user_id
                JOIN roles r ON r.id = urr.role_id
                JOIN restaurants rest ON rest.id = urr.restaurant_id
                WHERE urr.restaurant_id = :restaurantId
                  AND urr.deleted_at IS NULL
                  AND urr.assignment_status <> 'REMOVED'
                  AND u.deleted_at IS NULL
                """ + searchSql + statusSql + roleSql;
        long total = count(from, params);
        List<StaffMemberResponse> rows = jdbc.sql("""
                SELECT u.id user_id, u.email, user_full_name(u.first_name, u.second_name, u.first_lastname, u.second_lastname) full_name,
                       u.account_status::text account_status, u.last_login_at,
                       urr.restaurant_id, rest.name restaurant_name, r.id role_id, r.name role_name,
                       urr.assignment_status::text assignment_status
                """ + from + """
                ORDER BY u.first_name, u.first_lastname, u.email
                LIMIT :limit OFFSET :offset
                """)
                .params(params)
                .query((rs, rowNum) -> memberFromSingleAssignment(rs))
                .list();
        return PageResponse.of(rows, total, page, size);
    }

    @Override
    public StaffMemberResponse getRestaurantStaff(UUID restaurantId, UUID userId) {
        return jdbc.sql("""
                SELECT u.id user_id, u.email, user_full_name(u.first_name, u.second_name, u.first_lastname, u.second_lastname) full_name,
                       u.account_status::text account_status, u.last_login_at,
                       urr.restaurant_id, rest.name restaurant_name, r.id role_id, r.name role_name,
                       urr.assignment_status::text assignment_status
                FROM user_restaurant_roles urr
                JOIN users u ON u.id = urr.user_id
                JOIN restaurants rest ON rest.id = urr.restaurant_id
                JOIN roles r ON r.id = urr.role_id
                WHERE urr.restaurant_id = :restaurantId
                  AND urr.user_id = :userId
                  AND urr.deleted_at IS NULL
                  AND urr.assignment_status <> 'REMOVED'
                  AND u.deleted_at IS NULL
                """)
                .param("restaurantId", restaurantId)
                .param("userId", userId)
                .query((rs, rowNum) -> memberFromSingleAssignment(rs))
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Staff member not found."));
    }

    @Override
    @Transactional
    public StaffMemberResponse updateRestaurantStaff(
            UUID restaurantId, UUID userId, StaffAssignmentRequest request, UUID actorId) {
        if ("REMOVED".equalsIgnoreCase(request.status())) {
            removeRestaurantStaff(restaurantId, userId, actorId);
            return getAccountStaff(restaurantOwner(restaurantId), userId);
        }
        UUID roleId = request.roleId();
        if (roleId != null) {
            validateActiveRole(restaurantId, roleId);
        }
        jdbc.sql("""
                UPDATE user_restaurant_roles
                SET role_id = COALESCE(:roleId, role_id),
                    assignment_status = CAST(:status AS staff_assignment_status),
                    updated_at = NOW(),
                    updated_by = :actorId
                WHERE restaurant_id = :restaurantId
                  AND user_id = :userId
                  AND deleted_at IS NULL
                  AND assignment_status <> 'REMOVED'
                """)
                .param("roleId", roleId)
                .param("status", normalizeAssignmentStatus(request.status()))
                .param("actorId", actorId)
                .param("restaurantId", restaurantId)
                .param("userId", userId)
                .update();
        return getRestaurantStaff(restaurantId, userId);
    }

    @Override
    @Transactional
    public void removeRestaurantStaff(UUID restaurantId, UUID userId, UUID actorId) {
        jdbc.sql("""
                UPDATE user_restaurant_roles
                SET assignment_status = 'REMOVED',
                    deleted_at = NOW(),
                    deleted_by = :actorId,
                    updated_at = NOW(),
                    updated_by = :actorId
                WHERE restaurant_id = :restaurantId
                  AND user_id = :userId
                  AND deleted_at IS NULL
                """)
                .param("actorId", actorId)
                .param("restaurantId", restaurantId)
                .param("userId", userId)
                .update();
    }

    @Override
    public PageResponse<StaffInvitationResponse> listInvitations(UUID restaurantId, int page, int size, String search, String status) {
        page = Math.max(page, 0);
        size = normalizeSize(size);
        Map<String, Object> params = new HashMap<>();
        params.put("restaurantId", restaurantId);
        params.put("limit", size);
        params.put("offset", page * size);
        String searchSql = "";
        if (search != null && !search.isBlank()) {
            params.put("search", "%" + search.trim().toLowerCase() + "%");
            searchSql = " AND LOWER(si.email) LIKE :search ";
        }
        String statusSql = "";
        if (status != null && !status.isBlank()) {
            statusSql = " AND invitation_status(si.accepted_at, si.revoked_at, si.expires_at) = :status ";
            params.put("status", status.toUpperCase(Locale.ROOT));
        }
        String from = """
                FROM staff_invitations si
                JOIN restaurants rest ON rest.id = si.restaurant_id
                JOIN roles r ON r.id = si.role_id
                WHERE si.restaurant_id = :restaurantId AND si.deleted_at IS NULL
                """ + searchSql + statusSql;
        long total = count(from, params);
        List<StaffInvitationResponse> rows = jdbc.sql("""
                SELECT si.id, si.restaurant_id, rest.name restaurant_name, r.id role_id, r.name role_name,
                       si.email, invitation_status(si.accepted_at, si.revoked_at, si.expires_at) status,
                       si.expires_at, si.accepted_at, si.revoked_at, si.created_at
                """ + from + """
                ORDER BY si.created_at DESC
                LIMIT :limit OFFSET :offset
                """)
                .params(params)
                .query((rs, rowNum) -> invitationResponse(rs, null))
                .list();
        return PageResponse.of(rows, total, page, size);
    }

    @Override
    @Transactional
    public StaffInvitationResponse createInvitation(UUID restaurantId, StaffInvitationRequest request, UUID actorId) {
        String email = normalizeEmail(request.email());
        UUID ownerId = restaurantOwner(restaurantId);
        validateActiveRole(restaurantId, request.roleId());
        ensurePlanAllowsEmployee(ownerId);

        Optional<User> existingUser = userPersistence.findByEmail(email);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.getOwnerId() == null || !ownerId.equals(user.getOwnerId())) {
                throw new IllegalArgumentException("Email already belongs to another account.");
            }
            assignUserToRestaurant(user.getId(), restaurantId, request.roleId(), actorId);
            return null;
        }

        Token token = newToken();
        UUID id = jdbc.sql("""
                INSERT INTO staff_invitations (
                    owner_id, restaurant_id, role_id, email, token_hash, expires_at, created_by, updated_by
                )
                VALUES (:ownerId, :restaurantId, :roleId, :email, :tokenHash, :expiresAt, :actorId, :actorId)
                RETURNING id
                """)
                .param("ownerId", ownerId)
                .param("restaurantId", restaurantId)
                .param("roleId", request.roleId())
                .param("email", email)
                .param("tokenHash", token.hash())
                .param("expiresAt", Instant.now().plus(INVITATION_DAYS, ChronoUnit.DAYS))
                .param("actorId", actorId)
                .query(UUID.class)
                .single();
        return getInvitation(restaurantId, id, token.raw());
    }

    @Override
    @Transactional
    public StaffInvitationResponse regenerateInvitation(UUID restaurantId, UUID invitationId, UUID actorId) {
        StaffInvitationResponse current = getInvitation(restaurantId, invitationId, null);
        if (!"PENDING".equals(current.status()) && !"EXPIRED".equals(current.status())) {
            throw new IllegalArgumentException("Only pending or expired invitations can be regenerated.");
        }
        Token token = newToken();
        jdbc.sql("""
                UPDATE staff_invitations
                SET token_hash = :tokenHash,
                    expires_at = :expiresAt,
                    revoked_at = NULL,
                    revoked_by = NULL,
                    revoked_reason = NULL,
                    updated_at = NOW(),
                    updated_by = :actorId
                WHERE id = :invitationId AND restaurant_id = :restaurantId
                """)
                .param("tokenHash", token.hash())
                .param("expiresAt", Instant.now().plus(INVITATION_DAYS, ChronoUnit.DAYS))
                .param("actorId", actorId)
                .param("invitationId", invitationId)
                .param("restaurantId", restaurantId)
                .update();
        return getInvitation(restaurantId, invitationId, token.raw());
    }

    @Override
    @Transactional
    public StaffInvitationResponse revokeInvitation(UUID restaurantId, UUID invitationId, UUID actorId, String reason) {
        jdbc.sql("""
                UPDATE staff_invitations
                SET revoked_at = NOW(), revoked_by = :actorId, revoked_reason = :reason,
                    updated_at = NOW(), updated_by = :actorId
                WHERE id = :invitationId
                  AND restaurant_id = :restaurantId
                  AND accepted_at IS NULL
                  AND revoked_at IS NULL
                """)
                .param("actorId", actorId)
                .param("reason", reason)
                .param("invitationId", invitationId)
                .param("restaurantId", restaurantId)
                .update();
        return getInvitation(restaurantId, invitationId, null);
    }

    @Override
    public StaffInvitationResponse previewInvitation(String token) {
        return invitationByToken(token, null);
    }

    @Override
    @Transactional
    public StaffMemberResponse acceptInvitation(String token, AcceptStaffInvitationRequest request) {
        StaffInvitationRow invitation = invitationRowByToken(token);
        if (!"PENDING".equals(invitation.status())) {
            throw new IllegalArgumentException("Invitation is not pending.");
        }
        if (userPersistence.existsByEmail(invitation.email())) {
            throw new IllegalArgumentException("This email already has an account.");
        }
        if (request.username() != null && !request.username().isBlank()
                && userPersistence.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists.");
        }
        if (request.phoneNumber() != null && !request.phoneNumber().isBlank()
                && userPersistence.existsByPhoneNumber(request.phoneNumber())) {
            throw new IllegalArgumentException("Phone already exists.");
        }

        User user = User.builder()
                .email(invitation.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .secondName(request.secondName())
                .firstLastname(request.firstLastname())
                .secondLastname(request.secondLastname())
                .phoneNumber(blankToNull(request.phoneNumber()))
                .username(blankToNull(request.username()))
                .ownerId(invitation.ownerId())
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();
        User saved = userPersistence.save(user);
        assignUserToRestaurant(saved.getId(), invitation.restaurantId(), invitation.roleId(), invitation.ownerId());
        jdbc.sql("""
                UPDATE staff_invitations
                SET accepted_at = NOW(), accepted_by = :userId, updated_at = NOW(), updated_by = :userId
                WHERE id = :invitationId
                """)
                .param("userId", saved.getId())
                .param("invitationId", invitation.id())
                .update();
        return getRestaurantStaff(invitation.restaurantId(), saved.getId());
    }

    @Override
    public PageResponse<StaffMemberResponse> listAccountStaff(
            UUID ownerId, int page, int size, String search, String status, UUID restaurantId) {
        ensureOwner(ownerId);
        page = Math.max(page, 0);
        size = normalizeSize(size);
        Map<String, Object> params = new HashMap<>();
        params.put("ownerId", ownerId);
        params.put("limit", size);
        params.put("offset", page * size);
        params.put("restaurantId", restaurantId);
        String searchSql = "";
        if (search != null && !search.isBlank()) {
            params.put("search", "%" + search.trim().toLowerCase() + "%");
            searchSql = " AND (LOWER(u.email) LIKE :search OR LOWER(u.first_name || ' ' || u.first_lastname) LIKE :search) ";
        }
        String statusSql = status == null || status.isBlank() ? "" : " AND u.account_status = CAST(:status AS user_account_status) ";
        String restaurantSql = restaurantId == null ? "" : " AND EXISTS (SELECT 1 FROM user_restaurant_roles urr WHERE urr.user_id = u.id AND urr.restaurant_id = :restaurantId AND urr.assignment_status <> 'REMOVED' AND urr.deleted_at IS NULL) ";
        String from = """
                FROM users u
                WHERE u.owner_id = :ownerId AND u.deleted_at IS NULL
                """ + searchSql + statusSql + restaurantSql;
        long total = count(from, params);
        List<UUID> ids = jdbc.sql("SELECT u.id " + from + " ORDER BY u.first_name, u.first_lastname, u.email LIMIT :limit OFFSET :offset")
                .params(params)
                .query(UUID.class)
                .list();
        List<StaffMemberResponse> rows = ids.stream()
                .map(id -> getAccountStaff(ownerId, id))
                .toList();
        return PageResponse.of(rows, total, page, size);
    }

    @Override
    public StaffMemberResponse getAccountStaff(UUID ownerId, UUID userId) {
        ensureOwner(ownerId);
        StaffMemberResponse base = jdbc.sql("""
                SELECT u.id user_id, u.email, user_full_name(u.first_name, u.second_name, u.first_lastname, u.second_lastname) full_name,
                       u.account_status::text account_status, u.last_login_at
                FROM users u
                WHERE u.id = :userId AND u.owner_id = :ownerId AND u.deleted_at IS NULL
                """)
                .param("ownerId", ownerId)
                .param("userId", userId)
                .query((rs, rowNum) -> new StaffMemberResponse(
                        uuid(rs, "user_id"),
                        rs.getString("email"),
                        rs.getString("full_name"),
                        rs.getString("account_status"),
                        rs.getObject("last_login_at", Instant.class),
                        List.of()))
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Staff member not found."));
        List<StaffMemberResponse.Assignment> assignments = assignmentsForUser(userId);
        return new StaffMemberResponse(base.userId(), base.email(), base.fullName(),
                base.accountStatus(), base.lastLoginAt(), assignments);
    }

    @Override
    @Transactional
    public StaffMemberResponse updateAccountStatus(UUID ownerId, UUID userId, AccountStatusRequest request) {
        ensureOwner(ownerId);
        String status = normalizeAccountStatus(request.status());
        jdbc.sql("""
                UPDATE users
                SET account_status = CAST(:status AS user_account_status), updated_at = NOW(), updated_by = :ownerId
                WHERE id = :userId AND owner_id = :ownerId AND deleted_at IS NULL
                """)
                .param("status", status)
                .param("ownerId", ownerId)
                .param("userId", userId)
                .update();
        return getAccountStaff(ownerId, userId);
    }
    private PermissionCatalogResponse catalog(PermissionModule module, String label, PermissionAction... actions) {
        return new PermissionCatalogResponse(module, label, List.of(actions));
    }
    private void ensurePlanAllowsEmployee(UUID ownerId) {
        int limit = jdbc.sql("""
                SELECT COALESCE((sp.features->>'maxEmployees')::int, 100) max_employees
                FROM users u
                LEFT JOIN subscription_plans sp ON sp.id = u.subscription_plan_id
                WHERE u.id = :ownerId
                """)
                .param("ownerId", ownerId)
                .query(Integer.class)
                .single();
        boolean multiUser = jdbc.sql("""
                SELECT COALESCE((sp.features->>'multiUserAccess')::boolean, TRUE)
                FROM users u
                LEFT JOIN subscription_plans sp ON sp.id = u.subscription_plan_id
                WHERE u.id = :ownerId
                """)
                .param("ownerId", ownerId)
                .query(Boolean.class)
                .single();
        if (!multiUser) {
            throw new IllegalArgumentException("Current plan does not allow staff access.");
        }
        long used = jdbc.sql("""
                SELECT (
                    SELECT COUNT(DISTINCT u.id)
                    FROM users u
                    WHERE u.owner_id = :ownerId
                      AND u.deleted_at IS NULL
                ) + (
                    SELECT COUNT(*)
                    FROM staff_invitations si
                    WHERE si.owner_id = :ownerId
                      AND si.accepted_at IS NULL
                      AND si.revoked_at IS NULL
                      AND si.deleted_at IS NULL
                      AND si.expires_at > NOW()
                )
                """)
                .param("ownerId", ownerId)
                .query(Long.class)
                .single();
        if (used >= limit) {
            throw new IllegalArgumentException("Employee limit reached for current plan.");
        }
    }

    private UUID restaurantOwner(UUID restaurantId) {
        return jdbc.sql("SELECT owner_id FROM restaurants WHERE id = :restaurantId AND deleted_at IS NULL")
                .param("restaurantId", restaurantId)
                .query(UUID.class)
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found."));
    }

    private void ensureOwner(UUID ownerId) {
        boolean owner = jdbc.sql("""
                SELECT COUNT(*) > 0 FROM users
                WHERE id = :ownerId AND owner_id IS NULL AND deleted_at IS NULL
                """)
                .param("ownerId", ownerId)
                .query(Boolean.class)
                .single();
        if (!owner) {
            throw new IllegalArgumentException("Only owner users can perform this operation.");
        }
    }
    private void validateActiveRole(UUID restaurantId, UUID roleId) {
        restaurantOwner(restaurantId);
        boolean exists = jdbc.sql("""
                SELECT COUNT(*) > 0 FROM roles
                WHERE id = :roleId
                  AND restaurant_id IS NULL
                  AND is_assignable = TRUE
                  AND deleted_at IS NULL
                  AND is_active = TRUE
                """)
                .param("roleId", roleId)
                .query(Boolean.class)
                .single();
        if (!exists) {
            throw new IllegalArgumentException("Assignable active role not found.");
        }
    }
    private void assignUserToRestaurant(UUID userId, UUID restaurantId, UUID roleId, UUID actorId) {
        validateActiveRole(restaurantId, roleId);
        boolean exists = jdbc.sql("""
                SELECT COUNT(*) > 0 FROM user_restaurant_roles
                WHERE user_id = :userId AND restaurant_id = :restaurantId
                """)
                .param("userId", userId)
                .param("restaurantId", restaurantId)
                .query(Boolean.class)
                .single();
        if (exists) {
            jdbc.sql("""
                    UPDATE user_restaurant_roles
                    SET role_id = :roleId,
                        assignment_status = 'ACTIVE',
                        deleted_at = NULL,
                        deleted_by = NULL,
                        updated_at = NOW(),
                        updated_by = :actorId
                    WHERE user_id = :userId AND restaurant_id = :restaurantId
                    """)
                    .param("roleId", roleId)
                    .param("actorId", actorId)
                    .param("userId", userId)
                    .param("restaurantId", restaurantId)
                    .update();
        } else {
            jdbc.sql("""
                    INSERT INTO user_restaurant_roles (
                        user_id, restaurant_id, role_id, assignment_status, created_by, updated_by
                    )
                    VALUES (:userId, :restaurantId, :roleId, 'ACTIVE', :actorId, :actorId)
                    """)
                    .param("userId", userId)
                    .param("restaurantId", restaurantId)
                    .param("roleId", roleId)
                    .param("actorId", actorId)
                    .update();
        }
    }

    private StaffInvitationResponse getInvitation(UUID restaurantId, UUID invitationId, String token) {
        return jdbc.sql("""
                SELECT si.id, si.restaurant_id, rest.name restaurant_name, r.id role_id, r.name role_name,
                       si.email, invitation_status(si.accepted_at, si.revoked_at, si.expires_at) status,
                       si.expires_at, si.accepted_at, si.revoked_at, si.created_at
                FROM staff_invitations si
                JOIN restaurants rest ON rest.id = si.restaurant_id
                JOIN roles r ON r.id = si.role_id
                WHERE si.id = :invitationId AND si.restaurant_id = :restaurantId AND si.deleted_at IS NULL
                """)
                .param("invitationId", invitationId)
                .param("restaurantId", restaurantId)
                .query((rs, rowNum) -> invitationResponse(rs, token))
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found."));
    }

    private StaffInvitationResponse invitationByToken(String token, String rawToken) {
        StaffInvitationRow row = invitationRowByToken(token);
        return new StaffInvitationResponse(row.id(), row.restaurantId(), row.restaurantName(), row.roleId(),
                row.roleName(), row.email(), row.status(), row.expiresAt(), row.acceptedAt(), row.revokedAt(),
                rawToken == null ? null : "/invitations/" + rawToken, row.createdAt());
    }

    private StaffInvitationRow invitationRowByToken(String token) {
        return jdbc.sql("""
                SELECT si.id, si.owner_id, si.restaurant_id, rest.name restaurant_name, r.id role_id, r.name role_name,
                       si.email, invitation_status(si.accepted_at, si.revoked_at, si.expires_at) status,
                       si.expires_at, si.accepted_at, si.revoked_at, si.created_at
                FROM staff_invitations si
                JOIN restaurants rest ON rest.id = si.restaurant_id
                JOIN roles r ON r.id = si.role_id
                WHERE si.token_hash = :tokenHash AND si.deleted_at IS NULL
                """)
                .param("tokenHash", hashToken(token))
                .query((rs, rowNum) -> new StaffInvitationRow(
                        uuid(rs, "id"),
                        uuid(rs, "owner_id"),
                        uuid(rs, "restaurant_id"),
                        rs.getString("restaurant_name"),
                        uuid(rs, "role_id"),
                        rs.getString("role_name"),
                        rs.getString("email"),
                        rs.getString("status"),
                        rs.getObject("expires_at", Instant.class),
                        rs.getObject("accepted_at", Instant.class),
                        rs.getObject("revoked_at", Instant.class),
                        rs.getObject("created_at", Instant.class)))
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found."));
    }

    private StaffRoleResponse roleResponse(UUID id, String name, String permissions, String presetKey, boolean active, long assignedUsers) {
        return new StaffRoleResponse(id, name, permissionsFromJson(permissions), presetKey, active, assignedUsers);
    }

    private StaffMemberResponse memberFromSingleAssignment(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new StaffMemberResponse(
                uuid(rs, "user_id"),
                rs.getString("email"),
                rs.getString("full_name"),
                rs.getString("account_status"),
                rs.getObject("last_login_at", Instant.class),
                List.of(new StaffMemberResponse.Assignment(
                        uuid(rs, "restaurant_id"),
                        rs.getString("restaurant_name"),
                        uuid(rs, "role_id"),
                        rs.getString("role_name"),
                        rs.getString("assignment_status"))));
    }

    private List<StaffMemberResponse.Assignment> assignmentsForUser(UUID userId) {
        return jdbc.sql("""
                SELECT urr.restaurant_id, rest.name restaurant_name, r.id role_id, r.name role_name,
                       urr.assignment_status::text assignment_status
                FROM user_restaurant_roles urr
                JOIN restaurants rest ON rest.id = urr.restaurant_id
                JOIN roles r ON r.id = urr.role_id
                WHERE urr.user_id = :userId
                  AND urr.deleted_at IS NULL
                  AND urr.assignment_status <> 'REMOVED'
                ORDER BY rest.name
                """)
                .param("userId", userId)
                .query((rs, rowNum) -> new StaffMemberResponse.Assignment(
                        uuid(rs, "restaurant_id"),
                        rs.getString("restaurant_name"),
                        uuid(rs, "role_id"),
                        rs.getString("role_name"),
                        rs.getString("assignment_status")))
                .list();
    }

    private StaffInvitationResponse invitationResponse(java.sql.ResultSet rs, String token) throws java.sql.SQLException {
        return new StaffInvitationResponse(
                uuid(rs, "id"),
                uuid(rs, "restaurant_id"),
                rs.getString("restaurant_name"),
                uuid(rs, "role_id"),
                rs.getString("role_name"),
                rs.getString("email"),
                rs.getString("status"),
                rs.getObject("expires_at", Instant.class),
                rs.getObject("accepted_at", Instant.class),
                rs.getObject("revoked_at", Instant.class),
                token == null ? null : "/invitations/" + token,
                rs.getObject("created_at", Instant.class));
    }

    private long count(String from, Map<String, Object> params) {
        return jdbc.sql("SELECT COUNT(*) " + from)
                .params(params)
                .query(Long.class)
                .single();
    }

    private int normalizeSize(int size) {
        if (size <= 0) return 20;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeAssignmentStatus(String status) {
        String normalized = status.toUpperCase(Locale.ROOT);
        if (!Set.of("ACTIVE", "SUSPENDED").contains(normalized)) {
            throw new IllegalArgumentException("Invalid assignment status.");
        }
        return normalized;
    }

    private String normalizeAccountStatus(String status) {
        String normalized = status.toUpperCase(Locale.ROOT);
        if (!Set.of("ACTIVE", "SUSPENDED").contains(normalized)) {
            throw new IllegalArgumentException("Invalid account status.");
        }
        return normalized;
    }
    private Map<PermissionModule, List<PermissionAction>> permissionsFromJson(String json) {
        try {
            return MAPPER.readValue(json, PERMISSIONS_TYPE);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid permissions.", exception);
        }
    }

    private Token newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new Token(raw, hashToken(raw));
    }

    private String hashToken(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash invitation token.", exception);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private UUID uuid(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        return rs.getObject(column, UUID.class);
    }

    private record Token(String raw, String hash) {}

    private record StaffInvitationRow(
            UUID id,
            UUID ownerId,
            UUID restaurantId,
            String restaurantName,
            UUID roleId,
            String roleName,
            String email,
            String status,
            Instant expiresAt,
            Instant acceptedAt,
            Instant revokedAt,
            Instant createdAt) {
    }
}
