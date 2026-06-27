package com.beet.backend.modules.report.inventory.domain.usecase;

import com.beet.backend.modules.report.core.domain.api.ReportProvider;
import com.beet.backend.modules.report.core.domain.model.*;
import com.beet.backend.modules.report.core.domain.spi.ReportQueryPort;
import com.beet.backend.modules.report.inventory.domain.model.InventoryValuationReport;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class InventoryValuationReportProvider implements ReportProvider<InventoryValuationReport> {
    private final ReportQueryPort queryPort;

    public String key() { return "inventory.valuation"; }
    public ReportModule module() { return ReportModule.INVENTORY; }
    public ReportResultType resultType() { return ReportResultType.METRICS; }
    public Set<ReportContext> contexts() { return Set.of(ReportContext.RESTAURANT, ReportContext.ACCOUNT); }
    public PermissionModule requiredPermission() { return PermissionModule.INVENTORY; }
    public List<ReportFilterDefinition> filters() { return List.of(); }
    public Set<String> sortableFields() { return Set.of(); }
    public InventoryValuationReport generate(ReportQuery query) { return queryPort.inventoryValuation(query); }
}
