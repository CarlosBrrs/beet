package com.beet.backend.modules.documenttype.application.dto;

import java.util.UUID;

public record DocumentTypeResponse(
        UUID id,
        String name,
        String description) {
}
