package com.beet.backend.modules.{moduleName}.application.dto;

import lombok.Builder;
import java.util.UUID;

@Builder public record{Aggregate}Response(UUID id,String name
// Add other response fields
){}
