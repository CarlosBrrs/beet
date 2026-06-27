package com.beet.backend.modules.report.core.domain.usecase;

import com.beet.backend.modules.report.core.domain.api.ReportProvider;
import com.beet.backend.modules.report.core.domain.model.*;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportRegistryTest {

    @Test
    void shouldDiscoverAndExecuteProviderWithoutCentralSwitch() {
        EmptyResult expected = new EmptyResult();
        ReportProvider<EmptyResult> provider = provider("sales.custom", expected);
        ReportRegistry registry = new ReportRegistry(List.of(provider));
        ReportQuery query = new ReportQuery(
                new ReportScope(ReportContext.RESTAURANT, List.of(UUID.randomUUID())),
                LocalDate.now(), LocalDate.now(), ReportGrouping.DAY,
                0, 20, null, null, null);

        assertSame(expected, registry.execute("sales.custom", query, EmptyResult.class));
    }

    @Test
    void shouldRejectDuplicateProviderKeys() {
        assertThrows(IllegalStateException.class, () -> new ReportRegistry(List.of(
                provider("duplicate", new EmptyResult()),
                provider("duplicate", new EmptyResult()))));
    }

    private ReportProvider<EmptyResult> provider(String key, EmptyResult result) {
        return new ReportProvider<>() {
            public String key() { return key; }
            public ReportModule module() { return ReportModule.SALES; }
            public ReportResultType resultType() { return ReportResultType.METRICS; }
            public Set<ReportContext> contexts() { return Set.of(ReportContext.RESTAURANT); }
            public PermissionModule requiredPermission() { return PermissionModule.FINANCE; }
            public List<ReportFilterDefinition> filters() { return List.of(); }
            public Set<String> sortableFields() { return Set.of(); }
            public EmptyResult generate(ReportQuery query) { return result; }
        };
    }

    private record EmptyResult() implements ReportResult {
    }
}
