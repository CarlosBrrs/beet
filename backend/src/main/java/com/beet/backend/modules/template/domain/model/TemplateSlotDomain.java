package com.beet.backend.modules.template.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A slot within a Template (e.g. "Choose your protein").
 * min_selection = 0 → optional slot.
 * max_selection → maximum number of options that can be chosen.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class TemplateSlotDomain {

    private UUID id;
    private UUID templateId;
    private String name;
    private int minSelection; // 0 = optional, >0 = required
    private int maxSelection;
    private int sortOrder;

    @Builder.Default
    private List<SlotOptionDomain> options = new ArrayList<>();
}
