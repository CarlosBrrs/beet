package com.beet.backend.modules.report.core.domain.api;

import com.beet.backend.modules.report.core.domain.model.ReportContext;
import com.beet.backend.modules.report.core.domain.model.ReportFilterDefinition;
import com.beet.backend.modules.report.core.domain.model.ReportModule;
import com.beet.backend.modules.report.core.domain.model.ReportQuery;
import com.beet.backend.modules.report.core.domain.model.ReportResult;
import com.beet.backend.modules.report.core.domain.model.ReportResultType;
import com.beet.backend.modules.role.domain.model.PermissionModule;

import java.util.List;
import java.util.Set;

public interface ReportProvider<R extends ReportResult> {
    String key();

    ReportModule module();

    ReportResultType resultType();

    Set<ReportContext> contexts();

    PermissionModule requiredPermission();

    default Set<PermissionModule> additionalPermissions() {
        return Set.of();
    }

    List<ReportFilterDefinition> filters();

    Set<String> sortableFields();

    R generate(ReportQuery query);
}
