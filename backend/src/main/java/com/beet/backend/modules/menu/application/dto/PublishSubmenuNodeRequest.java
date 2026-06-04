package com.beet.backend.modules.menu.application.dto;

import com.beet.backend.modules.menu.domain.model.SubmenuNodeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PublishSubmenuNodeRequest(
        @NotNull SubmenuNodeType nodeType,
        @NotNull UUID referenceId,
        @Min(0) Integer sortOrder) {
}
