package com.beet.backend.modules.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PosTemplateSlotDomain {
    private UUID slotId;
    private String name;
    private int minSelection;
    private int maxSelection;
    private int sortOrder;

    @Builder.Default
    private List<PosTemplateOptionDomain> options = new ArrayList<>();
}
