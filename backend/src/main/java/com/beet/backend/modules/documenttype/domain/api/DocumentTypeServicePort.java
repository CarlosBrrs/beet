package com.beet.backend.modules.documenttype.domain.api;

import com.beet.backend.modules.documenttype.domain.model.DocumentTypeDomain;
import java.util.List;

public interface DocumentTypeServicePort {
    List<DocumentTypeDomain> getDocumentTypesByCountry(String countryCode);
}
