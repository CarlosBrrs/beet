package com.beet.backend.modules.documenttype.domain.spi;

import com.beet.backend.modules.documenttype.domain.model.DocumentTypeDomain;
import java.util.List;

public interface DocumentTypePersistencePort {
    List<DocumentTypeDomain> findByCountryCode(String countryCode);
}
