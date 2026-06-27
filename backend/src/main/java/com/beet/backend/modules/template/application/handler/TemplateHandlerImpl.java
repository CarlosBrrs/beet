package com.beet.backend.modules.template.application.handler;

import com.beet.backend.modules.template.application.dto.*;
import com.beet.backend.modules.template.domain.api.TemplateServicePort;
import com.beet.backend.modules.template.domain.model.SlotOptionDomain;
import com.beet.backend.modules.template.domain.model.TemplateDomain;
import com.beet.backend.modules.template.domain.model.TemplateSlotDomain;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateHandlerImpl implements TemplateHandler {

        private final TemplateServicePort templateServicePort;

        @Override
        public ApiGenericResponse<TemplateResponse> createTemplate(UUID submenuId, UUID ownerId,
                        CreateTemplateRequest request) {
                TemplateDomain domain = toDomain(ownerId, request);
                TemplateDomain created = templateServicePort.createTemplate(submenuId, domain);
                return ApiGenericResponse.success(toResponse(created));
        }

        @Override public ApiGenericResponse<TemplateResponse> createTemplate(UUID restaurantId, CreateTemplateRequest request) {
                return ApiGenericResponse.success(toResponse(templateServicePort.createTemplate(toDomain(restaurantId, request))));
        }
        @Override public ApiGenericResponse<PageResponse<TemplateResponse>> findAll(
                        UUID restaurantId, int page, int size, String search) {
                PageResponse<TemplateDomain> result = templateServicePort.findAllPaged(restaurantId, page, size, search);
                return ApiGenericResponse.success(PageResponse.of(
                                result.content().stream().map(this::toResponse).toList(),
                                result.totalElements(),
                                result.number(),
                                result.size()));
        }
        @Override public ApiGenericResponse<TemplateResponse> setActive(UUID restaurantId, UUID templateId, boolean active) {
                return ApiGenericResponse.success(toResponse(templateServicePort.setActive(restaurantId, templateId, active, SecurityUtils.getAuthenticatedUserId())));
        }

        @Override
        public ApiGenericResponse<TemplateResponse> updateTemplate(
                        UUID restaurantId, UUID templateId, CreateTemplateRequest request) {
                TemplateDomain domain = toDomain(restaurantId, request);
                domain.setId(templateId);
                return ApiGenericResponse.success(toResponse(templateServicePort.updateTemplate(
                                restaurantId, domain, SecurityUtils.getAuthenticatedUserId())));
        }

        @Override
        public ApiGenericResponse<TemplateResponse> getById(UUID templateId) {
                return ApiGenericResponse.success(toResponse(templateServicePort.getById(templateId)));
        }

        @Override
        public ApiGenericResponse<TemplateResponse> getById(UUID restaurantId, UUID templateId) {
                return ApiGenericResponse.success(toResponse(templateServicePort.getById(restaurantId, templateId)));
        }

        @Override
        public ApiGenericResponse<Void> deleteTemplate(UUID templateId) {
                templateServicePort.deleteTemplate(templateId);
                return ApiGenericResponse.success(null);
        }

        // ----- Conversion helpers ------------------------------------------------

        private TemplateDomain toDomain(UUID ownerId, CreateTemplateRequest request) {
                List<TemplateSlotDomain> slots = request.slots().stream().map(slotReq -> TemplateSlotDomain.builder()
                                .name(slotReq.name())
                                .minSelection(slotReq.minSelection())
                                .maxSelection(slotReq.maxSelection())
                                .sortOrder(slotReq.sortOrder())
                                .options(slotReq.options().stream().map(optReq -> SlotOptionDomain.builder()
                                                .itemId(optReq.itemId())
                                                .surcharge(optReq.surcharge() != null ? optReq.surcharge()
                                                                : BigDecimal.ZERO)
                                                .isDefault(optReq.isDefault())
                                                .maxQuantity(optReq.maxQuantity())
                                                .sortOrder(optReq.sortOrder())
                                                .build()).collect(Collectors.toList()))
                                .build()).collect(Collectors.toList());

                return TemplateDomain.builder()
                                .restaurantId(ownerId)
                                .name(request.name())
                                .description(request.description())
                                .basePrice(request.basePrice())
                                .isActive(true)
                                .createdBy(SecurityUtils.getAuthenticatedUserId())
                                .updatedBy(SecurityUtils.getAuthenticatedUserId())
                                .slots(slots)
                                .build();
        }

        private TemplateResponse toResponse(TemplateDomain d) {
                return new TemplateResponse(
                                d.getId(),
                                d.getRestaurantId(),
                                d.getName(),
                                d.getDescription(),
                                d.getBasePrice(),
                                d.isActive(),
                                d.isPublished(),
                                d.getSlots().stream().map(slot -> new TemplateResponse.SlotResponse(
                                                slot.getId(),
                                                slot.getName(),
                                                slot.getMinSelection(),
                                                slot.getMaxSelection(),
                                                slot.getSortOrder(),
                                                slot.getOptions().stream()
                                                                .map(opt -> new TemplateResponse.SlotOptionResponse(
                                                                                opt.getId(),
                                                                                opt.getItemId(),
                                                                                opt.getSurcharge(),
                                                                                opt.isDefault(),
                                                                                opt.getMaxQuantity(),
                                                                                opt.getSortOrder()))
                                                                .collect(Collectors.toList())))
                                                .collect(Collectors.toList()),
                                d.getCreatedAt(),
                                d.getUpdatedAt());
        }
}
