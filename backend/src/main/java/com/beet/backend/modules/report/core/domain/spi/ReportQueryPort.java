package com.beet.backend.modules.report.core.domain.spi;

import com.beet.backend.modules.report.cash.domain.model.BusinessDayDetailReport;
import com.beet.backend.modules.report.cash.domain.model.BusinessDayReportRow;
import com.beet.backend.modules.report.catalog.domain.model.CatalogReportRow;
import com.beet.backend.modules.report.core.domain.model.ReportOverview;
import com.beet.backend.modules.report.core.domain.model.ReportPage;
import com.beet.backend.modules.report.core.domain.model.ReportQuery;
import com.beet.backend.modules.report.inventory.domain.model.InventoryConsumptionRow;
import com.beet.backend.modules.report.inventory.domain.model.InventoryValuationReport;
import com.beet.backend.modules.report.inventory.domain.model.LowStockReportRow;
import com.beet.backend.modules.report.payments.domain.model.PaymentMethodReportRow;
import com.beet.backend.modules.report.sales.domain.model.SalesSeriesReport;

import java.util.UUID;

public interface ReportQueryPort {
    ReportOverview overview(ReportQuery query);

    SalesSeriesReport salesSeries(ReportQuery query);

    ReportPage<PaymentMethodReportRow> paymentMethods(ReportQuery query);

    ReportPage<BusinessDayReportRow> businessDays(ReportQuery query);

    BusinessDayDetailReport businessDayDetail(ReportQuery query, UUID businessDayId);

    ReportPage<CatalogReportRow> catalog(ReportQuery query, String lineType);

    ReportPage<InventoryConsumptionRow> inventoryConsumption(ReportQuery query);

    InventoryValuationReport inventoryValuation(ReportQuery query);

    ReportPage<LowStockReportRow> lowStock(ReportQuery query);
}
