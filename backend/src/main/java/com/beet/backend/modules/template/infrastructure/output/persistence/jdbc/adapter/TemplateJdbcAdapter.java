package com.beet.backend.modules.template.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.template.domain.model.SlotOptionDomain;
import com.beet.backend.modules.template.domain.model.TemplateDomain;
import com.beet.backend.modules.template.domain.model.TemplateSlotDomain;
import com.beet.backend.modules.template.domain.spi.TemplatePersistencePort;
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
                INSERT INTO templates (owner_id, name, description, base_price, created_by, updated_by)
                VALUES (:restaurantId, :name, :description, :basePrice, :restaurantId, :restaurantId)
                RETURNING *
                """;
        TemplateDomain saved = jdbcClient.sql(sql)
                .param("restaurantId", template.getRestaurantId())
                .param("name", template.getName())
                .param("description", template.getDescription())
                .param("basePrice", template.getBasePrice())
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
                       updated_by = :restaurantId
                 WHERE id = :id
                RETURNING *
                """;
        TemplateDomain updated = jdbcClient.sql(sql)
                .param("id", template.getId())
                .param("restaurantId", template.getRestaurantId())
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
        jdbcClient.sql("""
                INSERT INTO submenu_nodes (submenu_id, node_type, template_id)
                VALUES (:submenuId, 'TEMPLATE', :templateId)
                """)
                .param("submenuId", submenuId)
                .param("templateId", templateId)
                .update();
    }

    // -----------------------------------------------------------------------
    // Slots & Options
    // -----------------------------------------------------------------------

    private TemplateSlotDomain saveSlot(UUID templateId, TemplateSlotDomain slot) {
        String sql = """
                INSERT INTO template_slots (template_id, name, min_selection, max_selection, sort_order)
                VALUES (:templateId, :name, :minSelection, :maxSelection, :sortOrder)
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
                INSERT INTO slot_options (slot_id, item_id, surcharge, is_default, sort_order)
                VALUES (:slotId, :itemId, :surcharge, :isDefault, :sortOrder)
                """;
        for (SlotOptionDomain opt : options) {
            jdbcClient.sql(sql)
                    .param("slotId", slotId)
                    .param("itemId", opt.getItemId())
                    .param("surcharge", opt.getSurcharge() != null ? opt.getSurcharge() : BigDecimal.ZERO)
                    .param("isDefault", opt.isDefault())
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
                .sortOrder(rs.getInt("sort_order"))
                .build();
    }
}
