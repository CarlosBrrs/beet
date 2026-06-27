package com.beet.backend.modules.menu.domain.model;

import java.util.UUID;

public record SubmenuNodeDomain(
        UUID id,
        UUID submenuId,
        SubmenuNodeType nodeType,
        UUID itemId,
        UUID templateId,
        int sortOrder) {
}
