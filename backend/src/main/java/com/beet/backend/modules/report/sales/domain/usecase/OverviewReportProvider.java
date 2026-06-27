package com.beet.backend.modules.report.sales.domain.usecase;

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
public class OverviewReportProvider implements ReportProvider<ReportOverview> {
    private final ReportQueryPort queryPort;

    public String key() { return "overview"; }
    public ReportModule module() { return ReportModule.SALES; }
    public ReportResultType resultType() { return ReportResultType.METRICS; }
    public Set<ReportContext> contexts() { return Set.of(ReportContext.RESTAURANT, ReportContext.ACCOUNT); }
    public PermissionModule requiredPermission() { return PermissionModule.FINANCE; }
    public List<ReportFilterDefinition> filters() { return List.of(dateFilter()); }
    public Set<String> sortableFields() { return Set.of(); }
    public ReportOverview generate(ReportQuery query) { return queryPort.overview(query); }

    private ReportFilterDefinition dateFilter() {
        return new ReportFilterDefinition("dateRange", "DATE_RANGE", List.of());
    }
}
