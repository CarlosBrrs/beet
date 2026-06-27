package com.beet.backend.modules.item.application.dto;

import java.util.List;
import java.util.UUID;

public record ProductDependenciesResponse(
        List<Publication> publications,
        List<TemplateUsage> templateUsages) {

    public record Publication(
            UUID menuId,
            String menuName,
            UUID submenuId,
            String submenuName) {
    }

    public record TemplateUsage(
            UUID templateId,
            String templateName,
            UUID slotId,
            String slotName) {
    }
}
