package com.beet.backend.modules.report.core.domain.usecase;

import com.beet.backend.modules.report.core.domain.api.ReportProvider;
import com.beet.backend.modules.report.core.domain.model.*;
import com.beet.backend.modules.report.core.domain.spi.ReportAccessGateway;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportUseCaseTest {
    @Mock
    private ReportAccessGateway accessGateway;

    @Test
    void shouldResolveAccountScopeAndApplySafeDefaults() {
        CapturingProvider provider = new CapturingProvider(Set.of("period"));
        ReportUseCase useCase = new ReportUseCase(new ReportRegistry(List.of(provider)), accessGateway);
        UUID userId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        when(accessGateway.accessibleRestaurants(
                userId, null, Set.of(PermissionModule.FINANCE)))
                .thenReturn(List.of(restaurantId));

        EmptyResult result = useCase.execute(
                provider.key(), ReportContext.ACCOUNT, userId, null,
                null, null, ReportGrouping.DAY, -5, 500,
                "period,desc", "  test  ", null, EmptyResult.class);

        assertNotNull(result);
        assertEquals(List.of(restaurantId), provider.query.scope().restaurantIds());
        assertEquals(0, provider.query.page());
        assertEquals(100, provider.query.size());
        assertEquals("test", provider.query.search());
        assertEquals(29, java.time.temporal.ChronoUnit.DAYS.between(
                provider.query.dateFrom(), provider.query.dateTo()));
        verify(accessGateway).accessibleRestaurants(
                userId, null, Set.of(PermissionModule.FINANCE));
    }

    @Test
    void shouldRejectRangesLongerThanOneYear() {
        CapturingProvider provider = new CapturingProvider(Set.of());
        ReportUseCase useCase = new ReportUseCase(new ReportRegistry(List.of(provider)), accessGateway);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                provider.key(), ReportContext.RESTAURANT, UUID.randomUUID(),
                List.of(UUID.randomUUID()), LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 2, 1), ReportGrouping.DAY,
                0, 20, null, null, null, EmptyResult.class));
    }

    @Test
    void shouldRejectSortFieldsNotDeclaredByProvider() {
        CapturingProvider provider = new CapturingProvider(Set.of("period"));
        ReportUseCase useCase = new ReportUseCase(new ReportRegistry(List.of(provider)), accessGateway);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                provider.key(), ReportContext.RESTAURANT, UUID.randomUUID(),
                List.of(UUID.randomUUID()), LocalDate.now(), LocalDate.now(),
                ReportGrouping.DAY, 0, 20, "unsafe,desc", null, null, EmptyResult.class));
    }

    private static final class CapturingProvider implements ReportProvider<EmptyResult> {
        private final Set<String> sortableFields;
        private ReportQuery query;

        private CapturingProvider(Set<String> sortableFields) {
            this.sortableFields = sortableFields;
        }

        public String key() { return "test.report"; }
        public ReportModule module() { return ReportModule.SALES; }
        public ReportResultType resultType() { return ReportResultType.METRICS; }
        public Set<ReportContext> contexts() {
            return Set.of(ReportContext.RESTAURANT, ReportContext.ACCOUNT);
        }
        public PermissionModule requiredPermission() { return PermissionModule.FINANCE; }
        public List<ReportFilterDefinition> filters() { return List.of(); }
        public Set<String> sortableFields() { return sortableFields; }
        public EmptyResult generate(ReportQuery query) {
            this.query = query;
            return new EmptyResult();
        }
    }

    private record EmptyResult() implements ReportResult {
    }
}
