package com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.mapper;

import com.beet.backend.modules.ingredient.domain.model.MasterIngredientDomain;
import com.beet.backend.modules.ingredient.domain.model.SupplierItemDomain;
import com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.aggregate.MasterIngredientAggregate;
import com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.aggregate.SupplierItemAggregate;
import org.springframework.stereotype.Component;

@Component
public class IngredientAggregateMapper {

    /* ── Master Ingredient ── */

    public MasterIngredientDomain toDomain(MasterIngredientAggregate agg) {
        if (agg == null)
            return null;
        return MasterIngredientDomain.builder()
                .id(agg.getId())
                .ownerId(agg.getOwnerId())
                .name(agg.getName())
                .baseUnitId(agg.getBaseUnitId())
                .activeSupplierItemId(agg.getActiveSupplierItemId())
                .build();
    }

    public MasterIngredientAggregate toAggregate(MasterIngredientDomain domain) {
        if (domain == null)
            return null;
        return MasterIngredientAggregate.builder()
                .id(domain.getId())
                .ownerId(domain.getOwnerId())
                .name(domain.getName())
                .baseUnitId(domain.getBaseUnitId())
                .activeSupplierItemId(domain.getActiveSupplierItemId())
                .build();
    }

    /* ── Supplier Item ── */

    public SupplierItemDomain toDomain(SupplierItemAggregate agg) {
        if (agg == null)
            return null;
        return SupplierItemDomain.builder()
                .id(agg.getId())
                .masterIngredientId(agg.getMasterIngredientId())
                .supplierId(agg.getSupplierId())
                .brandName(agg.getBrandName())
                .purchaseUnitName(agg.getPurchaseUnitName())
                .conversionFactor(agg.getConversionFactor())
                .lastCostBase(agg.getLastCostBase())
                .build();
    }

    public SupplierItemAggregate toAggregate(SupplierItemDomain domain) {
        if (domain == null)
            return null;
        return SupplierItemAggregate.builder()
                .id(domain.getId())
                .masterIngredientId(domain.getMasterIngredientId())
                .supplierId(domain.getSupplierId())
                .brandName(domain.getBrandName())
                .purchaseUnitName(domain.getPurchaseUnitName())
                .conversionFactor(domain.getConversionFactor())
                .lastCostBase(domain.getLastCostBase())
                .build();
    }
}
