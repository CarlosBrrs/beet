package com.beet.backend.modules.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PosCatalogEntryDomain {
    private UUID nodeId;
    private UUID menuId;
    private String menuName;
    private UUID submenuId;
    private String submenuName;
    private CatalogReferenceType referenceType;
    private UUID referenceId;
    private String name;
    private String description;
    private BigDecimal price;
    private boolean available;
    private boolean lowStock;
    private Integer maxAvailableUnits;
    private String unavailableReason;
    private int sortOrder;

    @Builder.Default
    private List<String> insufficientIngredients = new ArrayList<>();

    @Builder.Default
    private List<PosTemplateSlotDomain> slots = new ArrayList<>();
}
