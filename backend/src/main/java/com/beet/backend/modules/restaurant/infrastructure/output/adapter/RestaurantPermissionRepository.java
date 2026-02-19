package com.beet.backend.modules.restaurant.infrastructure.output.adapter;

import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

public interface RestaurantPermissionRepository extends ListCrudRepository<PermissionProjection, UUID> {

    @Query(value = """
            SELECT r.name AS role_name,
            urr.id AS id,
            r.id AS role_id,
            urr.user_id AS user_id,
            urr.restaurant_id AS restaurant_id,
            r.permissions AS permission
            FROM user_restaurant_roles urr
            JOIN roles r ON urr.role_id = r.id
            WHERE urr.user_id = :userId
            AND urr.restaurant_id = :restaurantId
            """, rowMapperClass = PermissionRowMapper.class)
    PermissionProjection findByRestaurantIdAndUserId(UUID restaurantId, UUID userId);

}
