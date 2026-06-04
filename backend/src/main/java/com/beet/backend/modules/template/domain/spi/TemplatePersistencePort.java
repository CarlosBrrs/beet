package com.beet.backend.modules.template.domain.spi;

import com.beet.backend.modules.template.domain.model.TemplateDomain;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface TemplatePersistencePort {

    TemplateDomain save(TemplateDomain template);

    TemplateDomain update(TemplateDomain template);

    Optional<TemplateDomain> findById(UUID id);

    boolean existsByNameAndRestaurant(String name, UUID restaurantId);

    void deleteById(UUID id);

    /** Create a submenu_nodes entry linking a TEMPLATE to a submenu. */
    void saveSubmenuNode(UUID submenuId, UUID templateId);

    List<TemplateDomain> findAll(UUID restaurantId);

    PageResponse<TemplateDomain> findAllPaged(UUID restaurantId, int page, int size, String search);

    void updateActivation(UUID restaurantId, UUID templateId, boolean active, UUID userId);

    boolean isPublished(UUID restaurantId, UUID templateId);
}
