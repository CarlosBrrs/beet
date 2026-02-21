package com.beet.backend.modules.supplier.domain.usecase;

import com.beet.backend.modules.supplier.domain.api.SupplierServicePort;
import com.beet.backend.modules.supplier.domain.exception.SupplierAlreadyExistsException;
import com.beet.backend.modules.supplier.domain.exception.SupplierNotFoundException;
import com.beet.backend.modules.supplier.domain.model.SupplierDomain;
import com.beet.backend.modules.supplier.domain.spi.SupplierPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierUseCase implements SupplierServicePort {

    private final SupplierPersistencePort persistencePort;

    @Override
    @Transactional
    public SupplierDomain findOrCreate(SupplierDomain candidate, UUID ownerId) {
        /* Existing supplier → just look it up */
        if (candidate.getId() != null) {
            return persistencePort.findById(candidate.getId())
                    .orElseThrow(() -> SupplierNotFoundException.forId(candidate.getId()));
        }

        /* New supplier → validate uniqueness, then persist */
        if (persistencePort.existsByOwnerAndDocument(
                ownerId, candidate.getDocumentTypeId(), candidate.getDocumentNumber())) {
            throw SupplierAlreadyExistsException.forDocument(candidate.getDocumentNumber());
        }

        candidate.setOwnerId(ownerId);
        candidate.setIsActive(true);
        return persistencePort.save(candidate);
    }
}
