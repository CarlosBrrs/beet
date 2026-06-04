package com.beet.backend.modules.template.domain.usecase;

import com.beet.backend.modules.item.domain.model.ItemClass;
import com.beet.backend.modules.item.domain.model.ItemDomain;
import com.beet.backend.modules.item.domain.spi.ItemPersistencePort;
import com.beet.backend.modules.template.domain.model.SlotOptionDomain;
import com.beet.backend.modules.template.domain.model.TemplateDomain;
import com.beet.backend.modules.template.domain.model.TemplateSlotDomain;
import com.beet.backend.modules.template.domain.spi.TemplatePersistencePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateUseCaseTest {

    @Mock
    private TemplatePersistencePort persistencePort;

    @Mock
    private ItemPersistencePort itemPersistencePort;

    @InjectMocks
    private TemplateUseCase useCase;

    @Test
    void shouldRejectTemplateWithoutSlots() {
        TemplateDomain template = TemplateDomain.builder()
                .restaurantId(UUID.randomUUID())
                .name("Combo")
                .slots(List.of())
                .build();

        assertThrows(IllegalArgumentException.class, () -> useCase.createTemplate(template));

        verify(persistencePort, never()).save(any());
    }

    @Test
    void shouldRejectOptionFromAnotherRestaurant() {
        UUID restaurantId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        TemplateDomain template = template(restaurantId, itemId);
        when(itemPersistencePort.findById(itemId)).thenReturn(Optional.of(
                ItemDomain.builder()
                        .id(itemId)
                        .restaurantId(UUID.randomUUID())
                        .itemClass(ItemClass.PRODUCT)
                        .isActive(true)
                        .isAvailableAsTemplateOption(true)
                        .build()));

        assertThrows(IllegalArgumentException.class, () -> useCase.createTemplate(template));

        verify(persistencePort, never()).save(any());
    }

    @Test
    void shouldNormalizeSingletonRequiredSlotAsDefault() {
        UUID restaurantId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        TemplateDomain template = template(restaurantId, itemId);
        when(itemPersistencePort.findById(itemId)).thenReturn(Optional.of(
                ItemDomain.builder()
                        .id(itemId)
                        .restaurantId(restaurantId)
                        .itemClass(ItemClass.PRODUCT)
                        .isActive(true)
                        .isAvailableAsTemplateOption(true)
                        .build()));
        when(persistencePort.save(template)).thenReturn(template);

        useCase.createTemplate(template);

        assertTrue(template.getSlots().get(0).getOptions().get(0).isDefault());
        verify(persistencePort).save(template);
    }

    private TemplateDomain template(UUID restaurantId, UUID itemId) {
        return TemplateDomain.builder()
                .restaurantId(restaurantId)
                .name("Combo")
                .basePrice(BigDecimal.ZERO)
                .slots(List.of(TemplateSlotDomain.builder()
                        .name("Empaque")
                        .minSelection(1)
                        .maxSelection(1)
                        .options(List.of(SlotOptionDomain.builder()
                                .itemId(itemId)
                                .surcharge(BigDecimal.ZERO)
                                .maxQuantity(1)
                                .build()))
                        .build()))
                .build();
    }
}
