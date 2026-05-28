package com.beet.backend.modules.menu.application.dto;

import com.beet.backend.modules.item.application.dto.ItemResponse;
import com.beet.backend.modules.menu.domain.model.SubmenuNodeType;
import com.beet.backend.modules.template.application.dto.TemplateResponse;

import java.util.UUID;

public record SubmenuNodeResponse(
        UUID id,
        UUID submenuId,
        SubmenuNodeType nodeType,
        UUID itemId,
        UUID templateId,
        int sortOrder,
        ItemResponse item,
        TemplateResponse template) {
}
