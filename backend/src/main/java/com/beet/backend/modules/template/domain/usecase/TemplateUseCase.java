package com.beet.backend.modules.template.domain.usecase;

import com.beet.backend.modules.template.domain.api.TemplateServicePort;
import com.beet.backend.modules.template.domain.exception.TemplateAlreadyExistsException;
import com.beet.backend.modules.template.domain.exception.TemplateNotFoundException;
import com.beet.backend.modules.template.domain.model.TemplateDomain;
import com.beet.backend.modules.template.domain.model.TemplateSlotDomain;
import com.beet.backend.modules.template.domain.spi.TemplatePersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TemplateUseCase implements TemplateServicePort {

    private final TemplatePersistencePort persistencePort;

    @Override
    @Transactional
    public TemplateDomain createTemplate(UUID submenuId, TemplateDomain template) {
        if (persistencePort.existsByNameAndRestaurant(template.getName(), template.getRestaurantId())) {
            throw TemplateAlreadyExistsException.forName(template.getName());
        }
        validateSlots(template);
        TemplateDomain saved = persistencePort.save(template);
        persistencePort.saveSubmenuNode(submenuId, saved.getId());
        return saved;
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
        return persistencePort.update(existing);
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
        return persistencePort.findById(id)
                .orElseThrow(() -> TemplateNotFoundException.forId(id));
    }

    /**
     * Business validation: each slot's minSelection must be <= maxSelection,
     * and each slot must have at least one option.
     */
    private void validateSlots(TemplateDomain template) {
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
        }
    }
}
