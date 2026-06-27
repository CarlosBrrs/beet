package com.beet.backend.modules.menu.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SubmenuResponse(
        UUID id,
        UUID menuId,
        String name,
        String description,
        Integer sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
