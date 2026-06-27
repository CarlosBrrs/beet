package com.beet.backend.modules.report.core.domain.model;

import java.util.List;

public record ReportFilterDefinition(String key, String type, List<String> options) {
}
