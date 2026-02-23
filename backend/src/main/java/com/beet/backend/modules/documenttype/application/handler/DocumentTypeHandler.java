package com.beet.backend.modules.documenttype.application.handler;

import com.beet.backend.modules.documenttype.application.dto.DocumentTypeResponse;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import java.util.List;

public interface DocumentTypeHandler {
    ApiGenericResponse<List<DocumentTypeResponse>> getDocumentTypesByCountry(String countryCode);
}
