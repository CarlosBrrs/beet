package com.beet.backend.modules.report.core.domain.model;

import java.util.List;

public record ReportPage<T>(List<T> content, long totalElements, int page, int size) implements ReportResult {
}
