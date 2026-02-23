package com.beet.backend.modules.documenttype.application.mapper;

import com.beet.backend.modules.documenttype.application.dto.DocumentTypeResponse;
import com.beet.backend.modules.documenttype.domain.model.DocumentTypeDomain;
import org.springframework.stereotype.Component;

@Component
public class DocumentTypeServiceMapper {

    public DocumentTypeResponse toResponse(DocumentTypeDomain domain) {
        return new DocumentTypeResponse(
                domain.getId(),
                domain.getName(),
                domain.getDescription());
    }
}
