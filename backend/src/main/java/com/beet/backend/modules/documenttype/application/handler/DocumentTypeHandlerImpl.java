package com.beet.backend.modules.documenttype.application.handler;

import com.beet.backend.modules.documenttype.application.dto.DocumentTypeResponse;
import com.beet.backend.modules.documenttype.application.mapper.DocumentTypeServiceMapper;
import com.beet.backend.modules.documenttype.domain.api.DocumentTypeServicePort;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentTypeHandlerImpl implements DocumentTypeHandler {

    private final DocumentTypeServicePort servicePort;
    private final DocumentTypeServiceMapper mapper;

    @Override
    public ApiGenericResponse<List<DocumentTypeResponse>> getDocumentTypesByCountry(String countryCode) {
        List<DocumentTypeResponse> items = servicePort.getDocumentTypesByCountry(countryCode).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        return ApiGenericResponse.success(items);
    }
}
