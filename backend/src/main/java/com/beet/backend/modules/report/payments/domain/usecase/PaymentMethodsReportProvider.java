package com.beet.backend.modules.report.payments.domain.usecase;

import com.beet.backend.modules.report.core.domain.api.ReportProvider;
import com.beet.backend.modules.report.core.domain.model.*;
import com.beet.backend.modules.report.core.domain.spi.ReportQueryPort;
import com.beet.backend.modules.report.payments.domain.model.PaymentMethodReportRow;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PaymentMethodsReportProvider implements ReportProvider<ReportPage<PaymentMethodReportRow>> {
    private final ReportQueryPort queryPort;

    public String key() { return "payments.methods"; }
    public ReportModule module() { return ReportModule.PAYMENTS; }
    public ReportResultType resultType() { return ReportResultType.TABLE; }
    public Set<ReportContext> contexts() { return Set.of(ReportContext.RESTAURANT, ReportContext.ACCOUNT); }
    public PermissionModule requiredPermission() { return PermissionModule.FINANCE; }
    public List<ReportFilterDefinition> filters() { return List.of(); }
    public Set<String> sortableFields() { return Set.of("collected", "name"); }
    public ReportPage<PaymentMethodReportRow> generate(ReportQuery query) { return queryPort.paymentMethods(query); }
}
