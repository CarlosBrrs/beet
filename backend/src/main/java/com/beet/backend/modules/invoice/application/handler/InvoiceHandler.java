package com.beet.backend.modules.invoice.application.handler;

import com.beet.backend.modules.invoice.application.dto.InvoiceDetailResponse;
import com.beet.backend.modules.invoice.application.dto.InvoiceResponse;
import com.beet.backend.modules.invoice.application.dto.RegisterInvoiceRequest;
import com.beet.backend.modules.invoice.application.dto.SupplierItemForInvoiceResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;

import java.util.List;
import java.util.UUID;

public interface InvoiceHandler {

    ApiGenericResponse<InvoiceResponse> register(UUID restaurantId, RegisterInvoiceRequest request);

    ApiGenericResponse<PageResponse<InvoiceResponse>> list(UUID restaurantId, int page, int size, String search);

    ApiGenericResponse<InvoiceDetailResponse> getDetail(UUID restaurantId, UUID invoiceId);

    ApiGenericResponse<List<SupplierItemForInvoiceResponse>> getSupplierItems(UUID supplierId);
}
