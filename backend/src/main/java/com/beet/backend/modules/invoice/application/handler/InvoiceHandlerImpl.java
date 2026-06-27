package com.beet.backend.modules.invoice.application.handler;

import com.beet.backend.modules.invoice.application.dto.*;
import com.beet.backend.modules.invoice.domain.api.InvoiceServicePort;
import com.beet.backend.modules.invoice.domain.model.InvoiceDomain;
import com.beet.backend.modules.invoice.domain.model.InvoiceItemDomain;
import com.beet.backend.modules.invoice.infrastructure.output.persistence.jdbc.adapter.InvoiceJdbcAdapter;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceHandlerImpl implements InvoiceHandler {

        private final InvoiceServicePort invoiceService;
        private final InvoiceJdbcAdapter invoiceAdapter;

        @Override
        public ApiGenericResponse<InvoiceResponse> register(UUID restaurantId, RegisterInvoiceRequest request) {
                UUID userId = SecurityUtils.getAuthenticatedUserId();
                UUID ownerId = SecurityUtils.getEffectiveOwnerId();

                // Map request to domain
                List<InvoiceItemDomain> items = request.items().stream()
                                .map(itemReq -> InvoiceItemDomain.builder()
                                                .supplierItemId(itemReq.supplierItemId())
                                                .quantityPurchased(itemReq.quantityPurchased())
                                                .unitPricePurchased(itemReq.unitPricePurchased())
                                                .taxPercentage(itemReq.taxPercentage() != null
                                                                ? itemReq.taxPercentage()
                                                                : (request.taxPercentage() != null
                                                                                ? request.taxPercentage()
                                                                                : new BigDecimal("19.00")))
                                                .conversionFactorUsed(itemReq.conversionFactorUsed())
                                                .build())
                                .collect(Collectors.toList());

                InvoiceDomain invoice = InvoiceDomain.builder()
                                .ownerId(ownerId)
                                .restaurantId(restaurantId)
                                .supplierId(request.supplierId())
                                .supplierInvoiceNumber(request.supplierInvoiceNumber())
                                .emissionDate(request.emissionDate())
                                .notes(request.notes())
                                .items(items)
                                .build();

                InvoiceDomain saved = invoiceService.registerInvoice(invoice, restaurantId, userId);

                InvoiceResponse response = new InvoiceResponse(
                                saved.getId(),
                                null, // Supplier name not resolved in register response
                                saved.getSupplierInvoiceNumber(),
                                saved.getEmissionDate(),
                                saved.getReceivedAt(),
                                saved.getTotalAmount(),
                                saved.getItems().size(),
                                saved.getStatus().name());

                return ApiGenericResponse.success(response);
        }

        @Override
        public ApiGenericResponse<PageResponse<InvoiceResponse>> list(UUID restaurantId, int page, int size,
                        String search) {
                // Use adapter's enriched paginated query (JOINs supplier name + item count)
                PageResponse<InvoiceDomain> pageResult = invoiceAdapter.findAllPaged(restaurantId, page, size, search);

                List<InvoiceResponse> content = pageResult.content().stream()
                                .map(inv -> new InvoiceResponse(
                                                inv.getId(),
                                                inv.getSupplierName(),
                                                inv.getSupplierInvoiceNumber(),
                                                inv.getEmissionDate(),
                                                inv.getReceivedAt(),
                                                inv.getTotalAmount(),
                                                inv.getItems() != null ? inv.getItems().size() : 0,
                                                inv.getStatus().name()))
                                .collect(Collectors.toList());

                return ApiGenericResponse.success(PageResponse.of(content, pageResult.totalElements(), page, size));
        }

        @Override
        public ApiGenericResponse<InvoiceDetailResponse> getDetail(UUID restaurantId, UUID invoiceId) {
                // Use adapter's enriched detail query (JOINs ingredient name, unit info, etc.)
                InvoiceDetailResponse detail = invoiceAdapter.findDetailById(invoiceId)
                                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

                return ApiGenericResponse.success(detail);
        }

        @Override
        public ApiGenericResponse<List<SupplierItemForInvoiceResponse>> getSupplierItems(UUID supplierId) {
                // Use adapter's enriched query (JOINs ingredient name + base unit)
                List<SupplierItemForInvoiceResponse> items = invoiceAdapter.findSupplierItemsForInvoice(supplierId);
                return ApiGenericResponse.success(items);
        }
}
