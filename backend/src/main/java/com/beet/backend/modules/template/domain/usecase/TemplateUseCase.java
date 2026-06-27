package com.beet.backend.modules.template.domain.usecase;

import com.beet.backend.modules.template.domain.api.TemplateServicePort;
import com.beet.backend.modules.template.domain.exception.TemplateAlreadyExistsException;
import com.beet.backend.modules.template.domain.exception.TemplateNotFoundException;
import com.beet.backend.modules.template.domain.model.TemplateDomain;
import com.beet.backend.modules.template.domain.model.TemplateSlotDomain;
import com.beet.backend.modules.template.domain.spi.TemplatePersistencePort;
import com.beet.backend.modules.item.domain.spi.ItemPersistencePort;
import com.beet.backend.modules.item.domain.model.ItemClass;
import com.beet.backend.modules.item.domain.model.ItemDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TemplateUseCase implements TemplateServicePort {

    private final TemplatePersistencePort persistencePort;
    private final ItemPersistencePort itemPersistencePort;

    @Override
    @Transactional
    public TemplateDomain createTemplate(UUID submenuId, TemplateDomain template) {
        TemplateDomain saved = createTemplate(template);
        persistencePort.saveSubmenuNode(submenuId, saved.getId());
        saved.setPublished(true);
        return saved;
    }

    @Override
    @Transactional
    public TemplateDomain createTemplate(TemplateDomain template) {
        if (persistencePort.existsByNameAndRestaurant(template.getName(), template.getRestaurantId())) {
            throw TemplateAlreadyExistsException.forName(template.getName());
        }
        validateSlots(template);
        return persistencePort.save(template);
    }

    @Override
    @Transactional
    public TemplateDomain updateTemplate(TemplateDomain template) {
        TemplateDomain existing = persistencePort.findById(template.getId())
                .orElseThrow(() -> TemplateNotFoundException.forId(template.getId()));

        if (!existing.getName().equalsIgnoreCase(template.getName()) &&
                persistencePort.existsByNameAndRestaurant(template.getName(), existing.getRestaurantId())) {
            throw TemplateAlreadyExistsException.forName(template.getName());
        }
        validateSlots(template);
        existing.setName(template.getName());
        existing.setDescription(template.getDescription());
        existing.setBasePrice(template.getBasePrice());
        existing.setSlots(template.getSlots());
        existing.setUpdatedBy(template.getUpdatedBy());
        return decorateCatalogState(persistencePort.update(existing));
    }

    @Override
    @Transactional
    public TemplateDomain updateTemplate(UUID restaurantId, TemplateDomain template, UUID userId) {
        getById(restaurantId, template.getId());
        template.setUpdatedBy(userId);
        return updateTemplate(template);
    }

    @Override
    @Transactional
    public void deleteTemplate(UUID id) {
        persistencePort.findById(id)
                .orElseThrow(() -> TemplateNotFoundException.forId(id));
        persistencePort.deleteById(id);
    }

    @Override
    public TemplateDomain getById(UUID id) {
        return decorateCatalogState(persistencePort.findById(id)
                .orElseThrow(() -> TemplateNotFoundException.forId(id)));
    }

    @Override
    public TemplateDomain getById(UUID restaurantId, UUID id) {
        return decorateCatalogState(persistencePort.findById(id)
                .filter(template -> restaurantId.equals(template.getRestaurantId()))
                .orElseThrow(() -> TemplateNotFoundException.forId(id)));
    }

    @Override public java.util.List<TemplateDomain> findAll(UUID restaurantId) {
        java.util.List<TemplateDomain> templates = persistencePort.findAll(restaurantId);
        templates.forEach(this::decorateCatalogState);
        return templates;
    }

    @Override
    public PageResponse<TemplateDomain> findAllPaged(UUID restaurantId, int page, int size, String search) {
        if (page < 0 || size < 1 || size > 200) {
            throw new IllegalArgumentException("Pagination requires page >= 0 and size between 1 and 200.");
        }
        PageResponse<TemplateDomain> templates = persistencePort.findAllPaged(restaurantId, page, size, search);
        templates.content().forEach(this::decorateCatalogState);
        return templates;
    }

    @Override @Transactional
    public TemplateDomain setActive(UUID restaurantId, UUID templateId, boolean active, UUID userId) {
        TemplateDomain template = persistencePort.findById(templateId)
                .filter(found -> restaurantId.equals(found.getRestaurantId()))
                .orElseThrow(() -> TemplateNotFoundException.forId(templateId));
        if (!active && persistencePort.isPublished(restaurantId, templateId)) {
            throw new IllegalArgumentException("Unpublish the template before deactivating it.");
        }
        persistencePort.updateActivation(restaurantId, templateId, active, userId);
        template.setActive(active);
        return decorateCatalogState(template);
    }

    private TemplateDomain decorateCatalogState(TemplateDomain template) {
        template.setPublished(persistencePort.isPublished(template.getRestaurantId(), template.getId()));
        return template;
    }

    /**
     * Business validation: each slot's minSelection must be <= maxSelection,
     * and each slot must have at least one option.
     */
    private void validateSlots(TemplateDomain template) {
        if (template.getSlots() == null || template.getSlots().isEmpty()) {
            throw new IllegalArgumentException("A template must have at least one slot.");
        }
        for (TemplateSlotDomain slot : template.getSlots()) {
            if (slot.getMinSelection() < 0) {
                throw new IllegalArgumentException(
                        "Slot '" + slot.getName() + "': minSelection must be >= 0");
            }
            if (slot.getMinSelection() > slot.getMaxSelection()) {
                throw new IllegalArgumentException(
                        "Slot '" + slot.getName() + "': minSelection must be <= maxSelection");
            }
            if (slot.getOptions().isEmpty()) {
                throw new IllegalArgumentException(
                        "Slot '" + slot.getName() + "' must have at least one option");
            }
            if (slot.getMaxSelection() < 1) {
                throw new IllegalArgumentException(
                        "Slot '" + slot.getName() + "': maxSelection must be >= 1");
            }
            java.util.Set<UUID> seen = new java.util.HashSet<>();
            int defaults = 0;
            for (var option : slot.getOptions()) {
                if (!seen.add(option.getItemId())) {
                    throw new IllegalArgumentException("Slot '" + slot.getName() + "' cannot repeat a product.");
                }
                if (option.getMaxQuantity() < 1 || option.getMaxQuantity() > slot.getMaxSelection()) {
                    throw new IllegalArgumentException("Slot option maxQuantity must be between 1 and maxSelection.");
                }
                if (option.getSurcharge() == null || option.getSurcharge().signum() < 0) {
                    throw new IllegalArgumentException("Slot option surcharge must be >= 0.");
                }
                ItemDomain item = itemPersistencePort.findById(option.getItemId())
                        .orElseThrow(() -> new IllegalArgumentException("Template option product not found."));
                if (item.getItemClass() != ItemClass.PRODUCT || !item.isActive()
                        || !item.isAvailableAsTemplateOption()
                        || !item.getRestaurantId().equals(template.getRestaurantId())) {
                    throw new IllegalArgumentException("Template options must be active eligible products from the same restaurant.");
                }
                if (option.isDefault()) defaults++;
            }
            if (defaults > slot.getMaxSelection()) {
                throw new IllegalArgumentException("Slot default selections exceed maxSelection.");
            }
            if (slot.getOptions().size() == 1 && slot.getMinSelection() == 1 && slot.getMaxSelection() == 1) {
                slot.getOptions().get(0).setDefault(true);
            }
        }
    }
}
