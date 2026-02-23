package com.beet.backend.modules.documenttype.infrastructure.input.rest;

import com.beet.backend.modules.documenttype.application.dto.DocumentTypeResponse;
import com.beet.backend.modules.documenttype.application.handler.DocumentTypeHandler;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/document-types")
@RequiredArgsConstructor
public class DocumentTypeController {

    private final DocumentTypeHandler handler;

    @GetMapping
    public ResponseEntity<ApiGenericResponse<List<DocumentTypeResponse>>> getDocumentTypes(
            @RequestParam(value = "countryCode", required = true) String countryCode) {
        return ResponseEntity.ok(handler.getDocumentTypesByCountry(countryCode));
    }
}
