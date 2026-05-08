package com.beet.backend.modules.template.domain.api;

import com.beet.backend.modules.template.domain.model.TemplateDomain;

import java.util.UUID;

public interface TemplateServicePort {

    TemplateDomain createTemplate(UUID submenuId, TemplateDomain template);

    TemplateDomain updateTemplate(TemplateDomain template);

    void deleteTemplate(UUID id);

    TemplateDomain getById(UUID id);
}
