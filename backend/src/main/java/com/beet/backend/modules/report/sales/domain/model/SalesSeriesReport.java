package com.beet.backend.modules.report.sales.domain.model;

import com.beet.backend.modules.report.core.domain.model.ReportResult;
import com.beet.backend.modules.report.core.domain.model.ReportSeriesPoint;

import java.util.List;

public record SalesSeriesReport(boolean provisional, List<ReportSeriesPoint> points) implements ReportResult {
}
