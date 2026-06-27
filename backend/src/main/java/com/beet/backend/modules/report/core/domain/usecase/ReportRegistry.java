package com.beet.backend.modules.report.core.domain.usecase;

import com.beet.backend.modules.report.core.domain.api.ReportProvider;
import com.beet.backend.modules.report.core.domain.model.ReportQuery;
import com.beet.backend.modules.report.core.domain.model.ReportResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReportRegistry {
    private final Map<String, ReportProvider<? extends ReportResult>> providers;

    public ReportRegistry(List<ReportProvider<? extends ReportResult>> providers) {
        Map<String, ReportProvider<? extends ReportResult>> discovered = new LinkedHashMap<>();
        for (ReportProvider<? extends ReportResult> provider : providers) {
            if (discovered.putIfAbsent(provider.key(), provider) != null) {
                throw new IllegalStateException("Duplicate report provider: " + provider.key());
            }
        }
        this.providers = Map.copyOf(discovered);
    }

    public <R extends ReportResult> R execute(String key, ReportQuery query, Class<R> resultType) {
        ReportProvider<? extends ReportResult> provider = providers.get(key);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown report: " + key);
        }
        if (!provider.contexts().contains(query.scope().context())) {
            throw new IllegalArgumentException("Report does not support this context: " + key);
        }
        ReportResult result = provider.generate(query);
        if (!resultType.isInstance(result)) {
            throw new IllegalStateException("Unexpected result type for report: " + key);
        }
        return resultType.cast(result);
    }

    public List<ReportProvider<? extends ReportResult>> providers() {
        return providers.values().stream().toList();
    }

    public ReportProvider<? extends ReportResult> provider(String key) {
        ReportProvider<? extends ReportResult> provider = providers.get(key);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown report: " + key);
        }
        return provider;
    }
}
