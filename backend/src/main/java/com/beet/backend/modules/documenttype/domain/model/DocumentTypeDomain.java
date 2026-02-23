package com.beet.backend.modules.documenttype.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.util.UUID;

@Getter
@Builder
public class DocumentTypeDomain {
    private UUID id;
    private String name;
    private String description;
}
