package com.beet.backend.modules.template.domain.api;

import com.beet.backend.modules.template.domain.model.TemplateDomain;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.util.UUID;
import java.util.List;

public interface TemplateServicePort {

    TemplateDomain createTemplate(UUID submenuId, TemplateDomain template);

    TemplateDomain updateTemplate(TemplateDomain template);

    TemplateDomain updateTemplate(UUID restaurantId, TemplateDomain template, UUID userId);

    void deleteTemplate(UUID id);

    TemplateDomain getById(UUID id);

    TemplateDomain getById(UUID restaurantId, UUID id);

    TemplateDomain createTemplate(TemplateDomain template);

    List<TemplateDomain> findAll(UUID restaurantId);

    PageResponse<TemplateDomain> findAllPaged(UUID restaurantId, int page, int size, String search);

    TemplateDomain setActive(UUID restaurantId, UUID templateId, boolean active, UUID userId);
}
