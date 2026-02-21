package com.beet.backend.modules.ingredient.domain.model;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
public class MasterIngredientDomain {
    private UUID id;
    private UUID ownerId;
    private String name;
    private UUID baseUnitId;
    private UUID activeSupplierItemId;
}
