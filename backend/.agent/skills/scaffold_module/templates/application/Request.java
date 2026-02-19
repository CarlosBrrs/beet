package com.beet.backend.modules.{moduleName}.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder public record{Aggregate}Request(@NotBlank(message="Name is required")String name
// Add other request fields
){}
