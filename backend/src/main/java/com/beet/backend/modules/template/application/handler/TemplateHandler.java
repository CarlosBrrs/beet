package com.beet.backend.modules.template.application.handler;

import com.beet.backend.modules.template.application.dto.CreateTemplateRequest;
import com.beet.backend.modules.template.application.dto.TemplateResponse;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.util.UUID;
import java.util.List;

public interface TemplateHandler {
    ApiGenericResponse<TemplateResponse> createTemplate(UUID submenuId, UUID ownerId, CreateTemplateRequest request);

    ApiGenericResponse<TemplateResponse> getById(UUID templateId);

    ApiGenericResponse<TemplateResponse> getById(UUID restaurantId, UUID templateId);

    ApiGenericResponse<TemplateResponse> updateTemplate(
            UUID restaurantId, UUID templateId, CreateTemplateRequest request);

    ApiGenericResponse<Void> deleteTemplate(UUID templateId);

    ApiGenericResponse<TemplateResponse> createTemplate(UUID restaurantId, CreateTemplateRequest request);
    ApiGenericResponse<PageResponse<TemplateResponse>> findAll(UUID restaurantId, int page, int size, String search);
    ApiGenericResponse<TemplateResponse> setActive(UUID restaurantId, UUID templateId, boolean active);
}
