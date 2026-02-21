package com.beet.backend.modules.ingredient.application.mapper;

import com.beet.backend.modules.ingredient.application.dto.CreateIngredientRequest;
import com.beet.backend.modules.ingredient.application.dto.IngredientResponse;
import com.beet.backend.modules.ingredient.domain.model.MasterIngredientDomain;
import com.beet.backend.modules.ingredient.domain.model.SupplierItemDomain;
import com.beet.backend.modules.supplier.domain.model.SupplierDomain;
import org.springframework.stereotype.Component;

@Component
public class IngredientServiceMapper {

    public MasterIngredientDomain toIngredientDomain(CreateIngredientRequest.MasterIngredientPayload payload) {
        return MasterIngredientDomain.builder()
                .name(payload.name())
                .baseUnitId(payload.baseUnitId())
                .build();
    }

    public SupplierDomain toSupplierDomain(CreateIngredientRequest.SupplierPayload payload) {
        return SupplierDomain.builder()
                .id(payload.id())
                .name(payload.name())
                .documentTypeId(payload.documentTypeId())
                .documentNumber(payload.documentNumber())
                .build();
    }

    public SupplierItemDomain toSupplierItemDomain(CreateIngredientRequest.SupplierItemPayload payload) {
        return SupplierItemDomain.builder()
                .brandName(payload.brandName())
                .purchaseUnitName(payload.purchaseUnitName())
                .build();
    }

    public IngredientResponse toResponse(MasterIngredientDomain ingredient, SupplierItemDomain supplierItem) {
        return new IngredientResponse(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getBaseUnitId(),
                ingredient.getActiveSupplierItemId(),
                new IngredientResponse.SupplierItemInfo(
                        supplierItem.getId(),
                        supplierItem.getSupplierId(),
                        supplierItem.getBrandName(),
                        supplierItem.getPurchaseUnitName(),
                        supplierItem.getConversionFactor(),
                        supplierItem.getLastCostBase()));
    }
}
