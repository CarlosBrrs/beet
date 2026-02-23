package com.beet.backend.modules.documenttype.domain.usecase;

import com.beet.backend.modules.documenttype.domain.api.DocumentTypeServicePort;
import com.beet.backend.modules.documenttype.domain.exception.InvalidDocumentTypeSearchException;
import com.beet.backend.modules.documenttype.domain.model.DocumentTypeDomain;
import com.beet.backend.modules.documenttype.domain.spi.DocumentTypePersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentTypeUseCase implements DocumentTypeServicePort {

    private final DocumentTypePersistencePort persistencePort;

    @Override
    public List<DocumentTypeDomain> getDocumentTypesByCountry(String countryCode) {
        if (countryCode == null || countryCode.trim().isEmpty()) {
            throw InvalidDocumentTypeSearchException.missingCountryCode();
        }
        return persistencePort.findByCountryCode(countryCode.trim().toUpperCase());
    }
}
