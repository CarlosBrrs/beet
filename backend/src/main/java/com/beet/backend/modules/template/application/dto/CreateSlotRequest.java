package com.beet.backend.modules.template.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateSlotRequest(

        @NotBlank(message = "Slot name is required") String name,

        @Min(0) int minSelection,

        @Min(1) int maxSelection,

        int sortOrder,

        @NotNull @Valid List<CreateSlotOptionRequest> options) {
}
