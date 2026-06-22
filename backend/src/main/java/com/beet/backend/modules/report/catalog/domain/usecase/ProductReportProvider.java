package com.beet.backend.modules.report.catalog.domain.usecase;

import com.beet.backend.modules.report.catalog.domain.model.CatalogReportRow;
import com.beet.backend.modules.report.core.domain.api.ReportProvider;
import com.beet.backend.modules.report.core.domain.model.*;
import com.beet.backend.modules.report.core.domain.spi.ReportQueryPort;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProductReportProvider implements ReportProvider<ReportPage<CatalogReportRow>> {
    private final ReportQueryPort queryPort;

    public String key() { return "catalog.products"; }
    public ReportModule module() { return ReportModule.CATALOG; }
    public ReportResultType resultType() { return ReportResultType.TABLE; }
    public Set<ReportContext> contexts() { return Set.of(ReportContext.RESTAURANT, ReportContext.ACCOUNT); }
    public PermissionModule requiredPermission() { return PermissionModule.FINANCE; }
    public List<ReportFilterDefinition> filters() { return List.of(); }
    public Set<String> sortableFields() { return Set.of("name", "quantity", "sales"); }
    public ReportPage<CatalogReportRow> generate(ReportQuery query) { return queryPort.catalog(query, "PRODUCT"); }
}
