package com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.repository;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import com.beet.backend.modules.role.domain.model.UserRoleDTO;
import com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.aggregate.RoleAggregate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleJdbcRepository extends ListCrudRepository<RoleAggregate, UUID> {

        @Query("SELECT id FROM roles WHERE (LOWER(name) = LOWER(:name) OR preset_key = UPPER(:name)) AND deleted_at IS NULL ORDER BY restaurant_id NULLS FIRST LIMIT 1")
        Optional<UUID> findIdByName(String name);

        @Query("""
                        SELECT COUNT(*) > 0
                        FROM user_restaurant_roles
                        WHERE user_id = :userId
                          AND restaurant_id = :restaurantId
                          AND role_id = :roleId
                          AND assignment_status = 'ACTIVE'
                          AND deleted_at IS NULL
                        """)
        boolean existsByUserIdAndRestaurantIdAndRoleId(UUID userId, UUID restaurantId, UUID roleId);

        @Modifying
        @Query("INSERT INTO user_restaurant_roles (user_id, restaurant_id, role_id, assignment_status, created_by, updated_by) VALUES (:userId, :restaurantId, :roleId, 'ACTIVE', :senderId, :senderId)")
        void assignRoleToUser(UUID userId, UUID restaurantId, UUID roleId, UUID senderId);

        @Query(value = """
                        SELECT urr.restaurant_id, r.name as role_name
                        FROM user_restaurant_roles urr
                        JOIN roles r ON urr.role_id = r.id
                        WHERE urr.user_id = :userId
                          AND urr.assignment_status = 'ACTIVE'
                          AND urr.deleted_at IS NULL
                          AND r.deleted_at IS NULL
                          AND r.is_active = TRUE
                        """, rowMapperClass = UserRoleRowMapper.class)
        List<UserRoleDTO> findUserRoles(UUID userId);

        @Query("""
                        SELECT r.*
                        FROM roles r
                        JOIN user_restaurant_roles urr ON urr.role_id = r.id
                        WHERE urr.user_id = :userId AND urr.restaurant_id = :restaurantId
                          AND urr.assignment_status = 'ACTIVE'
                          AND urr.deleted_at IS NULL
                          AND r.deleted_at IS NULL
                          AND r.is_active = TRUE
                        """)
        Optional<RoleAggregate> findRoleByUserIdAndRestaurantId(UUID userId, UUID restaurantId);

        /**
         * Fetches all role-permission assignments for a user.
         * Includes the role template's restaurant_id to distinguish global roles (null)
         * from custom restaurant roles.
         */
        @Query(value = """
                        SELECT urr.restaurant_id as urr_restaurant_id,
                               r.name as role_name,
                               r.preset_key as role_preset_key,
                               r.restaurant_id as role_template_restaurant_id,
                               r.permissions
                        FROM user_restaurant_roles urr
                        JOIN roles r ON urr.role_id = r.id
                        WHERE urr.user_id = :userId
                          AND urr.assignment_status = 'ACTIVE'
                          AND urr.deleted_at IS NULL
                          AND r.deleted_at IS NULL
                          AND r.is_active = TRUE
                        """, rowMapperClass = UserRolePermissionRowMapper.class)
        List<UserRolePermissionProjection> findAllRoleAssignmentsForUser(UUID userId);
}
