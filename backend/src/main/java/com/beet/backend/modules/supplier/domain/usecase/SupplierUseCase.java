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
        if (candidate.getId() != null) {
            return persistencePort.findByIdAndOwnerId(candidate.getId(), ownerId)
                    .orElseThrow(() -> SupplierNotFoundException.forId(candidate.getId()));
        }
        return create(candidate, ownerId);
    }

    @Override
    @Transactional
    public SupplierDomain create(SupplierDomain supplier, UUID ownerId) {
        validateRequired(supplier);
        if (persistencePort.existsByOwnerAndDocument(ownerId, supplier.getDocumentTypeId(), supplier.getDocumentNumber())) {
            throw SupplierAlreadyExistsException.forDocument(supplier.getDocumentNumber());
        }

        supplier.setOwnerId(ownerId);
        supplier.setName(cleanRequired(supplier.getName(), "Supplier name is required"));
        supplier.setDocumentNumber(cleanRequired(supplier.getDocumentNumber(), "Document number is required"));
        supplier.setContactName(cleanOptional(supplier.getContactName()));
        supplier.setEmail(cleanOptional(supplier.getEmail()));
        supplier.setPhone(cleanOptional(supplier.getPhone()));
        supplier.setAddress(cleanOptional(supplier.getAddress()));
        supplier.setIsActive(true);
        return persistencePort.save(supplier);
    }

    @Override
    @Transactional
    public SupplierDomain update(UUID supplierId, SupplierDomain supplier, UUID ownerId) {
        SupplierDomain existing = persistencePort.findByIdAndOwnerId(supplierId, ownerId)
                .orElseThrow(() -> SupplierNotFoundException.forId(supplierId));
        validateRequired(supplier);

        if (persistencePort.existsByOwnerAndDocumentExcludingId(
                ownerId, supplier.getDocumentTypeId(), supplier.getDocumentNumber(), supplierId)) {
            throw SupplierAlreadyExistsException.forDocument(supplier.getDocumentNumber());
        }

        existing.setName(cleanRequired(supplier.getName(), "Supplier name is required"));
        existing.setDocumentTypeId(supplier.getDocumentTypeId());
        existing.setDocumentNumber(cleanRequired(supplier.getDocumentNumber(), "Document number is required"));
        existing.setContactName(cleanOptional(supplier.getContactName()));
        existing.setEmail(cleanOptional(supplier.getEmail()));
        existing.setPhone(cleanOptional(supplier.getPhone()));
        existing.setAddress(cleanOptional(supplier.getAddress()));
        return persistencePort.save(existing);
    }

    @Override
    @Transactional
    public SupplierDomain setActive(UUID supplierId, boolean isActive, UUID ownerId) {
        SupplierDomain existing = persistencePort.findByIdAndOwnerId(supplierId, ownerId)
                .orElseThrow(() -> SupplierNotFoundException.forId(supplierId));
        if (!isActive && persistencePort.hasBlockingReferences(supplierId)) {
            throw new IllegalArgumentException("Supplier cannot be deactivated while it has purchases or ingredient presentations linked.");
        }
        persistencePort.updateActive(supplierId, ownerId, isActive);
        existing.setIsActive(isActive);
        return existing;
    }

    @Override
    @Transactional
    public void delete(UUID supplierId, UUID ownerId, UUID actorId) {
        persistencePort.findByIdAndOwnerId(supplierId, ownerId)
                .orElseThrow(() -> SupplierNotFoundException.forId(supplierId));
        if (persistencePort.hasBlockingReferences(supplierId)) {
            throw new IllegalArgumentException("Supplier cannot be deleted while it has purchases or ingredient presentations linked.");
        }
        persistencePort.softDelete(supplierId, ownerId, actorId);
    }

    private void validateRequired(SupplierDomain supplier) {
        if (supplier.getDocumentTypeId() == null) {
            throw new IllegalArgumentException("Document type is required");
        }
        cleanRequired(supplier.getName(), "Supplier name is required");
        cleanRequired(supplier.getDocumentNumber(), "Document number is required");
    }

    private String cleanRequired(String value, String message) {
        String cleaned = cleanOptional(value);
        if (cleaned == null) {
            throw new IllegalArgumentException(message);
        }
        return cleaned;
    }

    private String cleanOptional(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
