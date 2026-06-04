package com.beet.backend.modules.item.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.item.domain.model.ItemClass;
import com.beet.backend.modules.item.domain.model.ItemDomain;
import com.beet.backend.modules.item.domain.model.RecipeLineDomain;
import com.beet.backend.modules.item.domain.model.RecipeLineSource;
import com.beet.backend.modules.item.domain.model.ProductDependenciesDomain;
import com.beet.backend.modules.item.domain.spi.ItemPersistencePort;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ItemJdbcAdapter implements ItemPersistencePort {

    private final JdbcClient jdbcClient;

    // -----------------------------------------------------------------------
    // Items CRUD
    // -----------------------------------------------------------------------

    @Override
    public ItemDomain save(ItemDomain item) {
        String sql = """
                INSERT INTO items
                    (restaurant_id, class, name, description, is_inventory_tracked,
                     yield_qty, yield_unit_id, sale_price, theoretical_cost, is_active, is_available_as_template_option,
                     created_by, updated_by)
                VALUES
                    (:restaurantId, :class::item_class, :name, :description, :tracked,
                     :yieldQty, :yieldUnitId, :salePrice, :theoreticalCost, :isActive, :isTemplateOption,
                     :createdBy, :updatedBy)
                RETURNING *
                """;
        return jdbcClient.sql(sql)
                .param("restaurantId", item.getRestaurantId())
                .param("class", item.getItemClass().name())
                .param("name", item.getName())
                .param("description", item.getDescription())
                .param("tracked", item.isInventoryTracked())
                .param("yieldQty", item.getYieldQty())
                .param("yieldUnitId", item.getYieldUnitId())
                .param("salePrice", item.getSalePrice())
                .param("theoreticalCost", item.getTheoreticalCost())
                .param("isActive", item.isActive())
                .param("isTemplateOption", item.isAvailableAsTemplateOption())
                .param("createdBy", item.getCreatedBy())
                .param("updatedBy", item.getUpdatedBy())
                .query(this::mapItem)
                .single();
    }

    @Override
    public ItemDomain update(ItemDomain item) {
        String sql = """
                   UPDATE items
                      SET name = :name,
                          description = :description,
                          yield_qty = :yieldQty,
                          yield_unit_id = :yieldUnitId,
                          sale_price = :salePrice,
                          theoretical_cost = :theoreticalCost,
                          is_available_as_template_option = :isTemplateOption,
                          updated_at = NOW(),
                          updated_by = :updatedBy
                    WHERE id = :id AND deleted_at IS NULL
                RETURNING *
                   """;
        return jdbcClient.sql(sql)
                .param("id", item.getId())
                .param("name", item.getName())
                .param("description", item.getDescription())
                .param("yieldQty", item.getYieldQty())
                .param("yieldUnitId", item.getYieldUnitId())
                .param("salePrice", item.getSalePrice())
                .param("theoreticalCost", item.getTheoreticalCost())
                .param("isTemplateOption", item.isAvailableAsTemplateOption())
                .param("updatedBy", item.getUpdatedBy())
                .query(this::mapItem)
                .single();
    }

    @Override
    public Optional<ItemDomain> findById(UUID id) {
        return jdbcClient.sql("SELECT * FROM items WHERE id = :id AND deleted_at IS NULL")
                .param("id", id)
                .query(this::mapItem)
                .optional();
    }

    @Override
    public List<ItemDomain> findAllByRestaurantAndClass(UUID restaurantId, ItemClass itemClass) {
        return jdbcClient.sql(
                "SELECT * FROM items WHERE restaurant_id = :restaurantId AND class = :class::item_class AND deleted_at IS NULL ORDER BY name ASC")
                .param("restaurantId", restaurantId)
                .param("class", itemClass.name())
                .query(this::mapItem)
                .list();
    }

    @Override
    public PageResponse<ItemDomain> findAllByRestaurantAndClassPaged(
            UUID restaurantId, ItemClass itemClass, int page, int size, String search) {
        String searchClause = search == null || search.isBlank() ? "" : " AND LOWER(name) LIKE :search";
        String whereClause = """
                 FROM items
                WHERE restaurant_id = :restaurantId
                  AND class = :class::item_class
                  AND deleted_at IS NULL
                """ + searchClause;

        var countQuery = jdbcClient.sql("SELECT COUNT(1)" + whereClause)
                .param("restaurantId", restaurantId)
                .param("class", itemClass.name());
        var listQuery = jdbcClient.sql("SELECT *" + whereClause + " ORDER BY name ASC LIMIT :size OFFSET :offset")
                .param("restaurantId", restaurantId)
                .param("class", itemClass.name())
                .param("size", size)
                .param("offset", (long) page * size);
        if (!searchClause.isEmpty()) {
            String normalizedSearch = "%" + search.toLowerCase() + "%";
            countQuery.param("search", normalizedSearch);
            listQuery.param("search", normalizedSearch);
        }
        Long totalElements = countQuery.query(Long.class).single();
        List<ItemDomain> content = listQuery.query(this::mapItem).list();
        return PageResponse.of(content, totalElements == null ? 0 : totalElements, page, size);
    }

    @Override
    public boolean existsByNameAndRestaurant(String name, UUID restaurantId) {
        return jdbcClient.sql(
                "SELECT COUNT(1) FROM items WHERE restaurant_id = :restaurantId AND LOWER(name) = LOWER(:name) AND deleted_at IS NULL")
                .param("restaurantId", restaurantId)
                .param("name", name)
                .query(Integer.class).single() > 0;
    }

    @Override
    public void deleteById(UUID id) {
        jdbcClient.sql("UPDATE items SET deleted_at = NOW() WHERE id = :id")
                .param("id", id)
                .update();
    }

    // -----------------------------------------------------------------------
    // Recipe Lines CRUD
    // -----------------------------------------------------------------------

    @Override
    public RecipeLineDomain saveRecipeLine(RecipeLineDomain line) {
        String sql = """
                INSERT INTO recipe_lines
                    (parent_item_id, source, master_ingredient_id, child_item_id, quantity, unit_id, sort_order)
                VALUES
                    (:parentItemId, :source::recipe_line_source, :masterIngredientId, :childItemId, :quantity, :unitId, :sortOrder)
                RETURNING *
                """;
        return jdbcClient.sql(sql)
                .param("parentItemId", line.getParentItemId())
                .param("source", line.getSource().name())
                .param("masterIngredientId", line.getMasterIngredientId())
                .param("childItemId", line.getChildItemId())
                .param("quantity", line.getQuantity())
                .param("unitId", line.getUnitId())
                .param("sortOrder", line.getSortOrder())
                .query(this::mapRecipeLine)
                .single();
    }

    @Override
    public void deleteRecipeLinesByParent(UUID parentItemId) {
        jdbcClient.sql("DELETE FROM recipe_lines WHERE parent_item_id = :parentItemId")
                .param("parentItemId", parentItemId)
                .update();
    }

    @Override
    public List<RecipeLineDomain> findRecipeLinesByParent(UUID parentItemId) {
        return jdbcClient.sql("SELECT * FROM recipe_lines WHERE parent_item_id = :parentItemId ORDER BY sort_order ASC")
                .param("parentItemId", parentItemId)
                .query(this::mapRecipeLine)
                .list();
    }

    // -----------------------------------------------------------------------
    // Circular reference detection
    // -----------------------------------------------------------------------

    @Override
    public List<UUID> findAllPreparationDescendantIds(UUID itemId) {
        // Recursive CTE: walk down the BOM tree from itemId
        String sql = """
                WITH RECURSIVE descendants AS (
                    SELECT child_item_id AS id
                      FROM recipe_lines
                     WHERE parent_item_id = :itemId
                       AND source = 'PREPARATION'
                    UNION
                    SELECT rl.child_item_id
                      FROM recipe_lines rl
                      JOIN descendants d ON rl.parent_item_id = d.id
                     WHERE rl.source = 'PREPARATION'
                )
                SELECT id FROM descendants WHERE id IS NOT NULL
                """;
        return jdbcClient.sql(sql)
                .param("itemId", itemId)
                .query((rs, rn) -> rs.getObject("id", UUID.class))
                .list();
    }

    // -----------------------------------------------------------------------
    // Cost calculation helpers
    // -----------------------------------------------------------------------

    /**
     * Returns factor_to_base for the given unit (1 if it's already a base unit).
     */
    @Override
    public BigDecimal getUnitFactorToBase(UUID unitId) {
        String sql = """
                   SELECT COALESCE(uc.factor, 1) AS factor
                     FROM units u
                LEFT JOIN unit_conversions uc ON uc.from_unit_id = u.id
                    WHERE u.id = :unitId
                   """;
        return jdbcClient.sql(sql)
                .param("unitId", unitId)
                .query((rs, rn) -> rs.getBigDecimal("factor"))
                .optional()
                .orElse(BigDecimal.ONE);
    }

    @Override
    public Optional<UUID> findUnitIdByAbbreviation(String abbreviation) {
        return jdbcClient.sql("SELECT id FROM units WHERE abbreviation = :abbreviation")
                .param("abbreviation", abbreviation)
                .query((rs, rn) -> rs.getObject("id", UUID.class))
                .optional();
    }

    /**
     * Returns last_cost_base for a master_ingredient. Falls back to 0 if not yet
     * set.
     */
    @Override
    public BigDecimal getIngredientLastCostBase(UUID masterIngredientId) {
        String sql = """
                   SELECT COALESCE(si.last_cost_base, 0)
                     FROM master_ingredients mi
                LEFT JOIN supplier_items si ON mi.active_supplier_item_id = si.id
                    WHERE mi.id = :id
                   """;
        return jdbcClient.sql(sql)
                .param("id", masterIngredientId)
                .query((rs, rn) -> rs.getBigDecimal(1))
                .optional()
                .orElse(BigDecimal.ZERO);
    }

    // -----------------------------------------------------------------------
    // Submenu node creation
    // -----------------------------------------------------------------------

    @Override
    public void saveSubmenuNode(UUID submenuId, UUID itemId) {
        int updated = jdbcClient.sql("""
                INSERT INTO submenu_nodes (submenu_id, restaurant_id, node_type, item_id)
                SELECT s.id, s.restaurant_id, 'PRODUCT', :itemId
                  FROM submenus s
                  JOIN items i ON i.id = :itemId
                              AND i.restaurant_id = s.restaurant_id
                              AND i.class = 'PRODUCT'
                              AND i.is_active = TRUE
                              AND i.sale_price > 0
                              AND i.deleted_at IS NULL
                 WHERE s.id = :submenuId
                   AND NOT EXISTS (SELECT 1 FROM submenu_nodes WHERE item_id = :itemId)
                """)
                .param("submenuId", submenuId)
                .param("itemId", itemId)
                .update();
        if (updated == 0) {
            throw new IllegalArgumentException("The product cannot be published in this submenu.");
        }
    }

    @Override
    public List<ItemDomain> findItemsBySubmenu(UUID submenuId) {
        String sql = """
                SELECT i.*
                  FROM items i
                  JOIN submenu_nodes sn ON sn.item_id = i.id
                 WHERE sn.submenu_id = :submenuId
                   AND sn.node_type = 'PRODUCT'
                   AND i.deleted_at IS NULL
                 ORDER BY sn.sort_order ASC
                """;
        return jdbcClient.sql(sql)
                .param("submenuId", submenuId)
                .query(this::mapItem)
                .list();
    }

    @Override
    public List<ItemDomain> findTemplateOptions(UUID restaurantId) {
        return jdbcClient.sql("SELECT * FROM items WHERE restaurant_id=:restaurantId AND class='PRODUCT' AND is_active=true AND is_available_as_template_option=true AND deleted_at IS NULL ORDER BY name")
                .param("restaurantId", restaurantId).query(this::mapItem).list();
    }

    @Override
    public void updateActivation(UUID restaurantId, UUID itemId, boolean active, UUID userId) {
        jdbcClient.sql("UPDATE items SET is_active=:active, updated_by=:userId, updated_at=NOW() WHERE id=:id AND restaurant_id=:restaurantId AND class='PRODUCT' AND deleted_at IS NULL")
                .param("active", active).param("userId", userId).param("id", itemId).param("restaurantId", restaurantId).update();
    }

    @Override
    public boolean isPublished(UUID restaurantId, UUID itemId) {
        return jdbcClient.sql("SELECT EXISTS(SELECT 1 FROM submenu_nodes WHERE restaurant_id=:restaurantId AND item_id=:itemId)")
                .param("restaurantId", restaurantId).param("itemId", itemId).query(Boolean.class).single();
    }

    @Override
    public boolean isUsedAsTemplateOption(UUID restaurantId, UUID itemId) {
        return jdbcClient.sql("SELECT EXISTS(SELECT 1 FROM slot_options WHERE restaurant_id=:restaurantId AND item_id=:itemId)")
                .param("restaurantId", restaurantId).param("itemId", itemId).query(Boolean.class).single();
    }

    @Override
    public ProductDependenciesDomain findDependencies(UUID restaurantId, UUID itemId) {
        List<ProductDependenciesDomain.Publication> publications = jdbcClient.sql("""
                SELECT m.id AS menu_id, m.name AS menu_name,
                       s.id AS submenu_id, s.name AS submenu_name
                  FROM submenu_nodes n
                  JOIN submenus s ON s.id = n.submenu_id AND s.restaurant_id = n.restaurant_id
                  JOIN menus m ON m.id = s.menu_id AND m.restaurant_id = n.restaurant_id
                 WHERE n.restaurant_id = :restaurantId
                   AND n.item_id = :itemId
                 ORDER BY m.name, s.name
                """)
                .param("restaurantId", restaurantId)
                .param("itemId", itemId)
                .query((rs, rn) -> new ProductDependenciesDomain.Publication(
                        rs.getObject("menu_id", UUID.class),
                        rs.getString("menu_name"),
                        rs.getObject("submenu_id", UUID.class),
                        rs.getString("submenu_name")))
                .list();

        List<ProductDependenciesDomain.TemplateUsage> templateUsages = jdbcClient.sql("""
                SELECT t.id AS template_id, t.name AS template_name,
                       s.id AS slot_id, s.name AS slot_name
                  FROM slot_options o
                  JOIN template_slots s ON s.id = o.slot_id AND s.restaurant_id = o.restaurant_id
                  JOIN templates t ON t.id = s.template_id AND t.restaurant_id = o.restaurant_id
                 WHERE o.restaurant_id = :restaurantId
                   AND o.item_id = :itemId
                   AND t.deleted_at IS NULL
                 ORDER BY t.name, s.sort_order, s.name
                """)
                .param("restaurantId", restaurantId)
                .param("itemId", itemId)
                .query((rs, rn) -> new ProductDependenciesDomain.TemplateUsage(
                        rs.getObject("template_id", UUID.class),
                        rs.getString("template_name"),
                        rs.getObject("slot_id", UUID.class),
                        rs.getString("slot_name")))
                .list();

        return new ProductDependenciesDomain(publications, templateUsages);
    }

    // -----------------------------------------------------------------------
    // Mappers
    // -----------------------------------------------------------------------

    private ItemDomain mapItem(ResultSet rs, int rn) throws SQLException {
        return ItemDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .itemClass(ItemClass.valueOf(rs.getString("class")))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .isInventoryTracked(rs.getBoolean("is_inventory_tracked"))
                .yieldQty(rs.getBigDecimal("yield_qty"))
                .yieldUnitId(rs.getObject("yield_unit_id", UUID.class))
                .salePrice(rs.getBigDecimal("sale_price"))
                .theoreticalCost(rs.getBigDecimal("theoretical_cost"))
                .isActive(rs.getBoolean("is_active"))
                .isAvailableAsTemplateOption(rs.getBoolean("is_available_as_template_option"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .recipeLines(new ArrayList<>())
                .build();
    }

    private RecipeLineDomain mapRecipeLine(ResultSet rs, int rn) throws SQLException {
        return RecipeLineDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .parentItemId(rs.getObject("parent_item_id", UUID.class))
                .source(RecipeLineSource.valueOf(rs.getString("source")))
                .masterIngredientId(rs.getObject("master_ingredient_id", UUID.class))
                .childItemId(rs.getObject("child_item_id", UUID.class))
                .quantity(rs.getBigDecimal("quantity"))
                .unitId(rs.getObject("unit_id", UUID.class))
                .sortOrder(rs.getInt("sort_order"))
                .build();
    }
}
