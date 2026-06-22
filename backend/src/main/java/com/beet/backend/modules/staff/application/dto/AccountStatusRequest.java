package com.beet.backend.modules.staff.application.dto;

import jakarta.validation.constraints.NotNull;

public record AccountStatusRequest(@NotNull String status) {
}
