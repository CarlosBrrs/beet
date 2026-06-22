package com.beet.backend.modules.report.sales.domain.usecase;

import com.beet.backend.modules.report.core.domain.api.ReportProvider;
import com.beet.backend.modules.report.core.domain.model.*;
import com.beet.backend.modules.report.core.domain.spi.ReportQueryPort;
import com.beet.backend.modules.report.sales.domain.model.SalesSeriesReport;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SalesTimeseriesReportProvider implements ReportProvider<SalesSeriesReport> {
    private final ReportQueryPort queryPort;

    public String key() { return "sales.timeseries"; }
    public ReportModule module() { return ReportModule.SALES; }
    public ReportResultType resultType() { return ReportResultType.SERIES; }
    public Set<ReportContext> contexts() { return Set.of(ReportContext.RESTAURANT, ReportContext.ACCOUNT); }
    public PermissionModule requiredPermission() { return PermissionModule.FINANCE; }
    public List<ReportFilterDefinition> filters() {
        return List.of(new ReportFilterDefinition("grouping", "SELECT", List.of("DAY", "WEEK", "MONTH")));
    }
    public Set<String> sortableFields() { return Set.of("period"); }
    public SalesSeriesReport generate(ReportQuery query) { return queryPort.salesSeries(query); }
}
