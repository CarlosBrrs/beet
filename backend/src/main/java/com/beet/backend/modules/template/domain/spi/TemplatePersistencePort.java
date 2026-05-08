package com.beet.backend.modules.template.domain.spi;

import com.beet.backend.modules.template.domain.model.TemplateDomain;

import java.util.Optional;
import java.util.UUID;

public interface TemplatePersistencePort {

    TemplateDomain save(TemplateDomain template);

    TemplateDomain update(TemplateDomain template);

    Optional<TemplateDomain> findById(UUID id);

    boolean existsByNameAndRestaurant(String name, UUID restaurantId);

    void deleteById(UUID id);

    /** Create a submenu_nodes entry linking a TEMPLATE to a submenu. */
    void saveSubmenuNode(UUID submenuId, UUID templateId);
}
