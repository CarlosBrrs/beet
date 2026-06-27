package com.beet.backend.modules.invoice.domain.spi;

import com.beet.backend.modules.invoice.domain.model.InvoiceDomain;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for invoice operations.
 */
public interface InvoicePersistencePort {

    InvoiceDomain save(InvoiceDomain invoice);

    Optional<InvoiceDomain> findByIdWithItems(UUID invoiceId);

    PageResponse<InvoiceDomain> findAllPaged(UUID restaurantId, int page, int size, String search);
}
