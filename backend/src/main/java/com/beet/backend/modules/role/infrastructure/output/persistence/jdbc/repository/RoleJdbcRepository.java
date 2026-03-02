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

        @Query("SELECT id FROM roles WHERE name = :name")
        Optional<UUID> findIdByName(String name);

        @Query("SELECT COUNT(*) > 0 FROM user_restaurant_roles WHERE user_id = :userId AND restaurant_id = :restaurantId AND role_id = :roleId")
        boolean existsByUserIdAndRestaurantIdAndRoleId(UUID userId, UUID restaurantId, UUID roleId);

        @Modifying
        @Query("INSERT INTO user_restaurant_roles (user_id, restaurant_id, role_id, created_by, updated_by) VALUES (:userId, :restaurantId, :roleId, :senderId, :senderId)")
        void assignRoleToUser(UUID userId, UUID restaurantId, UUID roleId, UUID senderId);

        @Query(value = """
                        SELECT urr.restaurant_id, r.name as role_name
                        FROM user_restaurant_roles urr
                        JOIN roles r ON urr.role_id = r.id
                        WHERE urr.user_id = :userId
                        """, rowMapperClass = UserRoleRowMapper.class)
        List<UserRoleDTO> findUserRoles(UUID userId);

        @Query("""
                        SELECT r.*
                        FROM roles r
                        JOIN user_restaurant_roles urr ON urr.role_id = r.id
                        WHERE urr.user_id = :userId AND urr.restaurant_id = :restaurantId
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
                               r.restaurant_id as role_template_restaurant_id,
                               r.permissions
                        FROM user_restaurant_roles urr
                        JOIN roles r ON urr.role_id = r.id
                        WHERE urr.user_id = :userId
                        """, rowMapperClass = UserRolePermissionRowMapper.class)
        List<UserRolePermissionProjection> findAllRoleAssignmentsForUser(UUID userId);
}
