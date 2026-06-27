package com.beet.backend.modules.menu.application.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MenuResponse(
        UUID id,
        UUID restaurantId,
        String name,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<SubmenuResponse> submenus) {
}
