package com.beet.backend.modules.report.core.domain.model;

import java.math.BigDecimal;

public record ReportMetric(String key, String label, BigDecimal value, String format) {
}
