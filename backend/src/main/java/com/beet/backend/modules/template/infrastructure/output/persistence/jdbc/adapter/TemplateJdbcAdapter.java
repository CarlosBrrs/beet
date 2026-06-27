package com.beet.backend.modules.template.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.template.domain.model.SlotOptionDomain;
import com.beet.backend.modules.template.domain.model.TemplateDomain;
import com.beet.backend.modules.template.domain.model.TemplateSlotDomain;
import com.beet.backend.modules.template.domain.spi.TemplatePersistencePort;
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
public class TemplateJdbcAdapter implements TemplatePersistencePort {

    private final JdbcClient jdbcClient;

    // -----------------------------------------------------------------------
    // Template CRUD
    // -----------------------------------------------------------------------

    @Override
    public TemplateDomain save(TemplateDomain template) {
        String sql = """
                INSERT INTO templates (restaurant_id, name, description, base_price, is_active, created_by, updated_by)
                VALUES (:restaurantId, :name, :description, :basePrice, :isActive, :createdBy, :updatedBy)
                RETURNING *
                """;
        TemplateDomain saved = jdbcClient.sql(sql)
                .param("restaurantId", template.getRestaurantId())
                .param("name", template.getName())
                .param("description", template.getDescription())
                .param("basePrice", template.getBasePrice())
                .param("isActive", template.isActive())
                .param("createdBy", template.getCreatedBy())
                .param("updatedBy", template.getUpdatedBy())
                .query(this::mapTemplate)
                .single();

        // Cascade save slots and options
        for (int i = 0; i < template.getSlots().size(); i++) {
            TemplateSlotDomain slot = template.getSlots().get(i);
            slot.setSortOrder(i);
            TemplateSlotDomain savedSlot = saveSlot(saved.getId(), slot);
            saveOptions(savedSlot.getId(), slot.getOptions());
            saved.getSlots().add(savedSlot);
        }
        return saved;
    }

    @Override
    public TemplateDomain update(TemplateDomain template) {
        String sql = """
                UPDATE templates
                   SET name = :name,
                       description = :description,
                       base_price = :basePrice,
                       updated_at = NOW(),
                       updated_by = :updatedBy
                 WHERE id = :id
                RETURNING *
                """;
        TemplateDomain updated = jdbcClient.sql(sql)
                .param("id", template.getId())
                .param("updatedBy", template.getUpdatedBy())
                .param("name", template.getName())
                .param("description", template.getDescription())
                .param("basePrice", template.getBasePrice())
                .query(this::mapTemplate)
                .single();

        // Replace slots and options
        jdbcClient.sql("DELETE FROM template_slots WHERE template_id = :id")
                .param("id", template.getId()).update();

        for (TemplateSlotDomain slot : template.getSlots()) {
            TemplateSlotDomain savedSlot = saveSlot(updated.getId(), slot);
            saveOptions(savedSlot.getId(), slot.getOptions());
            updated.getSlots().add(savedSlot);
        }
        return updated;
    }

    @Override
    public Optional<TemplateDomain> findById(UUID id) {
        Optional<TemplateDomain> opt = jdbcClient.sql(
                "SELECT * FROM templates WHERE id = :id AND deleted_at IS NULL")
                .param("id", id)
                .query(this::mapTemplate)
                .optional();

        opt.ifPresent(t -> {
            List<TemplateSlotDomain> slots = findSlotsByTemplate(t.getId());
            t.setSlots(slots);
        });
        return opt;
    }

    @Override
    public boolean existsByNameAndRestaurant(String name, UUID restaurantId) {
        Integer count = jdbcClient.sql(
                "SELECT COUNT(1) FROM templates WHERE restaurant_id = :restaurantId AND LOWER(name) = LOWER(:name) AND deleted_at IS NULL")
                .param("restaurantId", restaurantId)
                .param("name", name)
                .query(Integer.class).single();
        return count != null && count > 0;
    }

    @Override
    public void deleteById(UUID id) {
        jdbcClient.sql("UPDATE templates SET deleted_at = NOW() WHERE id = :id")
                .param("id", id).update();
    }

    @Override
    public void saveSubmenuNode(UUID submenuId, UUID templateId) {
        int updated = jdbcClient.sql("""
                INSERT INTO submenu_nodes (submenu_id, restaurant_id, node_type, template_id)
                SELECT s.id, s.restaurant_id, 'TEMPLATE', :templateId
                  FROM submenus s
                  JOIN templates t ON t.id = :templateId
                                  AND t.restaurant_id = s.restaurant_id
                                  AND t.is_active = TRUE
                                  AND t.deleted_at IS NULL
                 WHERE s.id = :submenuId
                   AND NOT EXISTS (SELECT 1 FROM submenu_nodes WHERE template_id = :templateId)
                """)
                .param("submenuId", submenuId)
                .param("templateId", templateId)
                .update();
        if (updated == 0) {
            throw new IllegalArgumentException("The template cannot be published in this submenu.");
        }
    }

    @Override
    public List<TemplateDomain> findAll(UUID restaurantId) {
        List<TemplateDomain> templates = jdbcClient.sql("SELECT * FROM templates WHERE restaurant_id=:restaurantId AND deleted_at IS NULL ORDER BY name")
                .param("restaurantId", restaurantId).query(this::mapTemplate).list();
        templates.forEach(template -> template.setSlots(findSlotsByTemplate(template.getId())));
        return templates;
    }

    @Override
    public PageResponse<TemplateDomain> findAllPaged(UUID restaurantId, int page, int size, String search) {
        String searchClause = search == null || search.isBlank() ? "" : " AND LOWER(name) LIKE :search";
        String whereClause = """
                 FROM templates
                WHERE restaurant_id = :restaurantId
                  AND deleted_at IS NULL
                """ + searchClause;

        var countQuery = jdbcClient.sql("SELECT COUNT(1)" + whereClause)
                .param("restaurantId", restaurantId);
        var listQuery = jdbcClient.sql("SELECT *" + whereClause + " ORDER BY name ASC LIMIT :size OFFSET :offset")
                .param("restaurantId", restaurantId)
                .param("size", size)
                .param("offset", (long) page * size);
        if (!searchClause.isEmpty()) {
            String normalizedSearch = "%" + search.toLowerCase() + "%";
            countQuery.param("search", normalizedSearch);
            listQuery.param("search", normalizedSearch);
        }
        Long totalElements = countQuery.query(Long.class).single();
        List<TemplateDomain> content = listQuery.query(this::mapTemplate).list();
        content.forEach(template -> template.setSlots(findSlotsByTemplate(template.getId())));
        return PageResponse.of(content, totalElements == null ? 0 : totalElements, page, size);
    }

    @Override
    public void updateActivation(UUID restaurantId, UUID templateId, boolean active, UUID userId) {
        jdbcClient.sql("UPDATE templates SET is_active=:active, updated_by=:userId, updated_at=NOW() WHERE id=:id AND restaurant_id=:restaurantId AND deleted_at IS NULL")
                .param("active", active).param("userId", userId).param("id", templateId).param("restaurantId", restaurantId).update();
    }

    @Override
    public boolean isPublished(UUID restaurantId, UUID templateId) {
        return jdbcClient.sql("SELECT EXISTS(SELECT 1 FROM submenu_nodes WHERE restaurant_id=:restaurantId AND template_id=:templateId)")
                .param("restaurantId", restaurantId).param("templateId", templateId).query(Boolean.class).single();
    }

    // -----------------------------------------------------------------------
    // Slots & Options
    // -----------------------------------------------------------------------

    private TemplateSlotDomain saveSlot(UUID templateId, TemplateSlotDomain slot) {
        String sql = """
                INSERT INTO template_slots (template_id, restaurant_id, name, min_selection, max_selection, sort_order)
                SELECT :templateId, restaurant_id, :name, :minSelection, :maxSelection, :sortOrder
                  FROM templates WHERE id = :templateId
                RETURNING *
                """;
        return jdbcClient.sql(sql)
                .param("templateId", templateId)
                .param("name", slot.getName())
                .param("minSelection", slot.getMinSelection())
                .param("maxSelection", slot.getMaxSelection())
                .param("sortOrder", slot.getSortOrder())
                .query(this::mapSlot)
                .single();
    }

    private void saveOptions(UUID slotId, List<SlotOptionDomain> options) {
        String sql = """
                INSERT INTO slot_options (slot_id, restaurant_id, item_id, surcharge, is_default, max_quantity, sort_order)
                SELECT :slotId, restaurant_id, :itemId, :surcharge, :isDefault, :maxQuantity, :sortOrder
                  FROM template_slots WHERE id = :slotId
                """;
        for (SlotOptionDomain opt : options) {
            jdbcClient.sql(sql)
                    .param("slotId", slotId)
                    .param("itemId", opt.getItemId())
                    .param("surcharge", opt.getSurcharge() != null ? opt.getSurcharge() : BigDecimal.ZERO)
                    .param("isDefault", opt.isDefault())
                    .param("maxQuantity", opt.getMaxQuantity())
                    .param("sortOrder", opt.getSortOrder())
                    .update();
        }
    }

    private List<TemplateSlotDomain> findSlotsByTemplate(UUID templateId) {
        List<TemplateSlotDomain> slots = jdbcClient.sql(
                "SELECT * FROM template_slots WHERE template_id = :templateId ORDER BY sort_order ASC")
                .param("templateId", templateId)
                .query(this::mapSlot)
                .list();

        for (TemplateSlotDomain slot : slots) {
            List<SlotOptionDomain> options = jdbcClient.sql(
                    "SELECT * FROM slot_options WHERE slot_id = :slotId ORDER BY sort_order ASC")
                    .param("slotId", slot.getId())
                    .query(this::mapOption)
                    .list();
            slot.setOptions(options);
        }
        return slots;
    }

    // -----------------------------------------------------------------------
    // Mappers
    // -----------------------------------------------------------------------

    private TemplateDomain mapTemplate(ResultSet rs, int rn) throws SQLException {
        return TemplateDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .restaurantId(rs.getObject("restaurant_id", UUID.class))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .basePrice(rs.getBigDecimal("base_price"))
                .isActive(rs.getBoolean("is_active"))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .slots(new ArrayList<>())
                .build();
    }

    private TemplateSlotDomain mapSlot(ResultSet rs, int rn) throws SQLException {
        return TemplateSlotDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .templateId(rs.getObject("template_id", UUID.class))
                .name(rs.getString("name"))
                .minSelection(rs.getInt("min_selection"))
                .maxSelection(rs.getInt("max_selection"))
                .sortOrder(rs.getInt("sort_order"))
                .options(new ArrayList<>())
                .build();
    }

    private SlotOptionDomain mapOption(ResultSet rs, int rn) throws SQLException {
        return SlotOptionDomain.builder()
                .id(rs.getObject("id", UUID.class))
                .slotId(rs.getObject("slot_id", UUID.class))
                .itemId(rs.getObject("item_id", UUID.class))
                .surcharge(rs.getBigDecimal("surcharge"))
                .isDefault(rs.getBoolean("is_default"))
                .maxQuantity(rs.getInt("max_quantity"))
                .sortOrder(rs.getInt("sort_order"))
                .build();
    }
}
