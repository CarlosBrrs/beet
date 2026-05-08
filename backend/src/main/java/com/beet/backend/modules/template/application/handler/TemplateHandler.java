package com.beet.backend.modules.template.application.handler;

import com.beet.backend.modules.template.application.dto.CreateTemplateRequest;
import com.beet.backend.modules.template.application.dto.TemplateResponse;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;

import java.util.UUID;

public interface TemplateHandler {
    ApiGenericResponse<TemplateResponse> createTemplate(UUID submenuId, UUID ownerId, CreateTemplateRequest request);

    ApiGenericResponse<TemplateResponse> getById(UUID templateId);

    ApiGenericResponse<Void> deleteTemplate(UUID templateId);
}
