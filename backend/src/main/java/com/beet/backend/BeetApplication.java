package com.beet.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@SpringBootApplication
public class BeetApplication {

	public static void main(String[] args) {
		SpringApplication.run(BeetApplication.class, args);
	}

	/**
	 * TEMPORARY test data seeder — remove before production.
	 * Creates: owner (test1@mail.com), a restaurant, a MANAGER role, and two
	 * managers.
	 */
	@Bean
	CommandLineRunner seedTestData(NamedParameterJdbcTemplate jdbc, PasswordEncoder encoder) {
		return args -> {
			// Check if test owner already exists (idempotent)
			Integer count = jdbc.queryForObject(
					"SELECT COUNT(*) FROM users WHERE email = :email",
					new MapSqlParameterSource("email", "test1@mail.com"),
					Integer.class);

			if (count != null && count > 0) {
				System.out.println("⚡ Test data already exists, skipping seeder.");
				return;
			}

			String hashedPassword = encoder.encode("password");

			// ─── 1. New Owner (test1@mail.com) ─────────────────────────────────
			UUID ownerId = UUID.randomUUID();
			jdbc.update(
					"INSERT INTO users (id, email, password_hash, first_name, first_lastname, owner_id, created_by, updated_by) "
							+
							"VALUES (:id, :email, :pw, :firstName, :lastName, NULL, :id, :id)",
					new MapSqlParameterSource()
							.addValue("id", ownerId)
							.addValue("email", "test1@mail.com")
							.addValue("pw", hashedPassword)
							.addValue("firstName", "Test")
							.addValue("lastName", "Owner"));

			// ─── 2. New Restaurant for test1 owner ─────────────────────────────
			UUID restaurant2Id = UUID.randomUUID();
			jdbc.update(
					"INSERT INTO restaurants (id, name, address, operation_mode, owner_id, created_by, updated_by) " +
							"VALUES (:id, :name, :address, 'POSTPAID', :ownerId, :ownerId, :ownerId)",
					new MapSqlParameterSource()
							.addValue("id", restaurant2Id)
							.addValue("name", "Test Restaurant 2")
							.addValue("address", "456 Test Avenue")
							.addValue("ownerId", ownerId));

			// ─── 3. Assign OWNER role to test1 on their restaurant ─────────────
			UUID ownerRoleId = jdbc.queryForObject(
					"SELECT id FROM roles WHERE name = 'OWNER' LIMIT 1",
					new MapSqlParameterSource(), UUID.class);

			jdbc.update(
					"INSERT INTO user_restaurant_roles (id, user_id, restaurant_id, role_id, created_by, updated_by) " +
							"VALUES (:id, :userId, :restaurantId, :roleId, :userId, :userId)",
					new MapSqlParameterSource()
							.addValue("id", UUID.randomUUID())
							.addValue("userId", ownerId)
							.addValue("restaurantId", restaurant2Id)
							.addValue("roleId", ownerRoleId));

			// ─── 4. MANAGER role (global, with INVENTORY permissions) ──────────
			UUID managerRoleId = UUID.randomUUID();
			jdbc.update(
					"INSERT INTO roles (id, name, permissions, restaurant_id, created_by, updated_by) " +
							"VALUES (:id, :name, :permissions::jsonb, NULL, :createdBy, :createdBy)",
					new MapSqlParameterSource()
							.addValue("id", managerRoleId)
							.addValue("name", "MANAGER")
							.addValue("permissions",
							"""
									{"INVENTORY": ["VIEW", "ACTIVATE", "EDIT"], "RESTAURANTS": ["VIEW"], "CATALOG": ["VIEW"]}
									""")
							.addValue("createdBy", ownerId));

			// ─── 5. Find the first existing restaurant (from any owner) ────────
			UUID restaurant1Id = jdbc.queryForObject(
					"SELECT id FROM restaurants WHERE id != :excludeId ORDER BY created_at ASC LIMIT 1",
					new MapSqlParameterSource("excludeId", restaurant2Id),
					UUID.class);

			// ─── 6. Manager 1 — assigned to the first existing restaurant ──────
			UUID manager1Id = UUID.randomUUID();
			UUID restaurant1OwnerId = jdbc.queryForObject(
					"SELECT owner_id FROM restaurants WHERE id = :id",
					new MapSqlParameterSource("id", restaurant1Id), UUID.class);

			jdbc.update(
					"INSERT INTO users (id, email, password_hash, first_name, first_lastname, owner_id, created_by, updated_by) "
							+
							"VALUES (:id, :email, :pw, :firstName, :lastName, :ownerId, :ownerId, :ownerId)",
					new MapSqlParameterSource()
							.addValue("id", manager1Id)
							.addValue("email", "manager1@mail.com")
							.addValue("pw", hashedPassword)
							.addValue("firstName", "Manager")
							.addValue("lastName", "One")
							.addValue("ownerId", restaurant1OwnerId));

			jdbc.update(
					"INSERT INTO user_restaurant_roles (id, user_id, restaurant_id, role_id, created_by, updated_by) " +
							"VALUES (:id, :userId, :restaurantId, :roleId, :createdBy, :createdBy)",
					new MapSqlParameterSource()
							.addValue("id", UUID.randomUUID())
							.addValue("userId", manager1Id)
							.addValue("restaurantId", restaurant1Id)
							.addValue("roleId", managerRoleId)
							.addValue("createdBy", restaurant1OwnerId));

			// ─── 7. Manager 2 — assigned to the new test restaurant ────────────
			UUID manager2Id = UUID.randomUUID();
			jdbc.update(
					"INSERT INTO users (id, email, password_hash, first_name, first_lastname, owner_id, created_by, updated_by) "
							+
							"VALUES (:id, :email, :pw, :firstName, :lastName, :ownerId, :ownerId, :ownerId)",
					new MapSqlParameterSource()
							.addValue("id", manager2Id)
							.addValue("email", "manager2@mail.com")
							.addValue("pw", hashedPassword)
							.addValue("firstName", "Manager")
							.addValue("lastName", "Two")
							.addValue("ownerId", ownerId));

			jdbc.update(
					"INSERT INTO user_restaurant_roles (id, user_id, restaurant_id, role_id, created_by, updated_by) " +
							"VALUES (:id, :userId, :restaurantId, :roleId, :createdBy, :createdBy)",
					new MapSqlParameterSource()
							.addValue("id", UUID.randomUUID())
							.addValue("userId", manager2Id)
							.addValue("restaurantId", restaurant2Id)
							.addValue("roleId", managerRoleId)
							.addValue("createdBy", ownerId));

			System.out.println("✅ Test data seeded successfully!");
			System.out.println("   Owner:     test1@mail.com / password → Test Restaurant 2");
			System.out.println("   Manager 1: manager1@mail.com / password → Restaurant 1 (existing)");
			System.out.println("   Manager 2: manager2@mail.com / password → Test Restaurant 2 (new)");
		};
	}
}
