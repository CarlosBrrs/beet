package com.beet.backend.modules.menu.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.menu.domain.model.MenuDomain;
import com.beet.backend.modules.menu.domain.model.SubmenuDomain;
import com.beet.backend.modules.menu.domain.spi.MenuPersistencePort;
import com.beet.backend.modules.menu.domain.spi.SubmenuPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class MenuJdbcAdapter implements MenuPersistencePort, SubmenuPersistencePort {

    private final JdbcClient jdbcClient;

    // --- Menus ---

    @Override
    public MenuDomain save(MenuDomain menu) {
        String sql = """
                    INSERT INTO menus (restaurant_id, name, description)
                    VALUES (:restaurantId, :name, :description)
                    RETURNING id, restaurant_id, name, description, created_at, updated_at
                """;

        return jdbcClient.sql(sql)
                .param("restaurantId", menu.getRestaurantId())
                .param("name", menu.getName())
                .param("description", menu.getDescription() != null ? menu.getDescription() : "")
                .query(this::mapMenu)
                .single();
    }

    @Override
    public MenuDomain update(MenuDomain menu) {
        String sql = """
                    UPDATE menus
                    SET name = :name,
                        description = :description,
                        updated_at = NOW()
                    WHERE id = :id
                    RETURNING id, restaurant_id, name, description, created_at, updated_at
                """;

        return jdbcClient.sql(sql)
                .param("name", menu.getName())
                .param("description", menu.getDescription() != null ? menu.getDescription() : "")
                .param("id", menu.getId())
                .query(this::mapMenu)
                .single();
    }

    @Override
    public Optional<MenuDomain> findMenuById(UUID menuId) {
        String sql = "SELECT * FROM menus WHERE id = :id";
        return jdbcClient.sql(sql)
                .param("id", menuId)
                .query(this::mapMenu)
                .optional();
    }

    @Override
    public List<MenuDomain> findAllWithSubmenus(UUID restaurantId) {
        // Fetch menus
        String menusSql = "SELECT * FROM menus WHERE restaurant_id = :restaurantId ORDER BY name ASC";
        List<MenuDomain> menus = jdbcClient.sql(menusSql)
                .param("restaurantId", restaurantId)
                .query(this::mapMenu)
                .list();

        if (menus.isEmpty())
            return menus;

        // Fetch submenus for these menus
        List<UUID> menuIds = menus.stream().map(MenuDomain::getId).toList();
        String submenusSql = "SELECT * FROM submenus WHERE menu_id IN (:menuIds) ORDER BY sort_order ASC, name ASC";
        List<SubmenuDomain> submenus = jdbcClient.sql(submenusSql)
                .param("menuIds", menuIds)
                .query(this::mapSubmenu)
                .list();

        // Group submenus by menuId
        Map<UUID, List<SubmenuDomain>> submenusByMenuId = new HashMap<>();
        for (SubmenuDomain sm : submenus) {
            submenusByMenuId.computeIfAbsent(sm.getMenuId(), k -> new ArrayList<>()).add(sm);
        }

        // Assign to menus
        for (MenuDomain m : menus) {
            m.setSubmenus(submenusByMenuId.getOrDefault(m.getId(), new ArrayList<>()));
        }

        return menus;
    }

    @Override
    public boolean existsByNameInRestaurant(String name, UUID restaurantId) {
        String sql = "SELECT COUNT(1) FROM menus WHERE restaurant_id = :restaurantId AND LOWER(name) = LOWER(:name)";
        Integer count = jdbcClient.sql(sql)
                .param("restaurantId", restaurantId)
                .param("name", name)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    // --- Submenus ---

    @Override
    public SubmenuDomain save(SubmenuDomain submenu) {
        String sql = """
                    INSERT INTO submenus (menu_id, name, description, sort_order)
                    VALUES (:menuId, :name, :description, :sortOrder)
                    RETURNING id, menu_id, name, description, sort_order, created_at, updated_at
                """;

        return jdbcClient.sql(sql)
                .param("menuId", submenu.getMenuId())
                .param("name", submenu.getName())
                .param("description", submenu.getDescription() != null ? submenu.getDescription() : "")
                .param("sortOrder", submenu.getSortOrder() != null ? submenu.getSortOrder() : 0)
                .query(this::mapSubmenu)
                .single();
    }

    @Override
    public SubmenuDomain update(SubmenuDomain submenu) {
        String sql = """
                    UPDATE submenus
                    SET name = :name,
                        description = :description,
                        sort_order = :sortOrder,
                        updated_at = NOW()
                    WHERE id = :id
                    RETURNING id, menu_id, name, description, sort_order, created_at, updated_at
                """;

        return jdbcClient.sql(sql)
                .param("name", submenu.getName())
                .param("description", submenu.getDescription() != null ? submenu.getDescription() : "")
                .param("sortOrder", submenu.getSortOrder() != null ? submenu.getSortOrder() : 0)
                .param("id", submenu.getId())
                .query(this::mapSubmenu)
                .single();
    }

    @Override
    public Optional<SubmenuDomain> findSubmenuById(UUID submenuId) {
        String sql = "SELECT * FROM submenus WHERE id = :id";
        return jdbcClient.sql(sql)
                .param("id", submenuId)
                .query(this::mapSubmenu)
                .optional();
    }

    @Override
    public boolean existsByNameInMenu(String name, UUID menuId) {
        String sql = "SELECT COUNT(1) FROM submenus WHERE menu_id = :menuId AND LOWER(name) = LOWER(:name)";
        Integer count = jdbcClient.sql(sql)
                .param("menuId", menuId)
                .param("name", name)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    // --- Mappers ---

    private MenuDomain mapMenu(ResultSet rs, int rowNum) throws SQLException {
        return MenuDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .submenus(new ArrayList<>())
                .build();
    }

    private SubmenuDomain mapSubmenu(ResultSet rs, int rowNum) throws SQLException {
        return SubmenuDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .menuId(rs.getObject("menu_id", UUID.class))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .sortOrder(rs.getInt("sort_order"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .build();
    }
}
