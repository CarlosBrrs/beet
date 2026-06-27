package com.beet.backend.modules.report.cash.domain.usecase;

import com.beet.backend.modules.report.cash.domain.model.BusinessDayReportRow;
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
public class BusinessDaysReportProvider implements ReportProvider<ReportPage<BusinessDayReportRow>> {
    private final ReportQueryPort queryPort;

    public String key() { return "cash.business-days"; }
    public ReportModule module() { return ReportModule.CASH; }
    public ReportResultType resultType() { return ReportResultType.TABLE; }
    public Set<ReportContext> contexts() { return Set.of(ReportContext.RESTAURANT, ReportContext.ACCOUNT); }
    public PermissionModule requiredPermission() { return PermissionModule.CASH; }
    public Set<PermissionModule> additionalPermissions() { return Set.of(PermissionModule.FINANCE); }
    public List<ReportFilterDefinition> filters() { return List.of(); }
    public Set<String> sortableFields() { return Set.of("date"); }
    public ReportPage<BusinessDayReportRow> generate(ReportQuery query) { return queryPort.businessDays(query); }
}
