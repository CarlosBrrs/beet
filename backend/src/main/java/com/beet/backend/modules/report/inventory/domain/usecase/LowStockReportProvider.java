package com.beet.backend.modules.report.inventory.domain.usecase;

import com.beet.backend.modules.report.core.domain.api.ReportProvider;
import com.beet.backend.modules.report.core.domain.model.*;
import com.beet.backend.modules.report.core.domain.spi.ReportQueryPort;
import com.beet.backend.modules.report.inventory.domain.model.LowStockReportRow;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LowStockReportProvider implements ReportProvider<ReportPage<LowStockReportRow>> {
    private final ReportQueryPort queryPort;

    public String key() { return "inventory.low-stock"; }
    public ReportModule module() { return ReportModule.INVENTORY; }
    public ReportResultType resultType() { return ReportResultType.TABLE; }
    public Set<ReportContext> contexts() { return Set.of(ReportContext.RESTAURANT, ReportContext.ACCOUNT); }
    public PermissionModule requiredPermission() { return PermissionModule.INVENTORY; }
    public List<ReportFilterDefinition> filters() { return List.of(); }
    public Set<String> sortableFields() { return Set.of("shortage", "name"); }
    public ReportPage<LowStockReportRow> generate(ReportQuery query) { return queryPort.lowStock(query); }
}
