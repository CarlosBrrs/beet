package com.beet.backend.modules.report.cash.domain.usecase;

import com.beet.backend.modules.report.cash.domain.model.BusinessDayDetailReport;
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
public class BusinessDayDetailReportProvider implements ReportProvider<BusinessDayDetailReport> {
    private final ReportQueryPort queryPort;

    public String key() { return "cash.business-day-detail"; }
    public ReportModule module() { return ReportModule.CASH; }
    public ReportResultType resultType() { return ReportResultType.DETAIL; }
    public Set<ReportContext> contexts() { return Set.of(ReportContext.RESTAURANT); }
    public PermissionModule requiredPermission() { return PermissionModule.CASH; }
    public Set<PermissionModule> additionalPermissions() { return Set.of(PermissionModule.FINANCE); }
    public List<ReportFilterDefinition> filters() { return List.of(); }
    public Set<String> sortableFields() { return Set.of(); }
    public BusinessDayDetailReport generate(ReportQuery query) {
        if (query.resourceId() == null) throw new IllegalArgumentException("Business day id is required.");
        return queryPort.businessDayDetail(query, query.resourceId());
    }
}
