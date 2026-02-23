package com.beet.backend.modules.ingredient.domain.api;

import com.beet.backend.modules.ingredient.application.dto.IngredientDetailResponse;
import com.beet.backend.modules.ingredient.application.dto.IngredientListResponse;
import com.beet.backend.modules.ingredient.domain.model.MasterIngredientDomain;
import com.beet.backend.modules.ingredient.domain.model.SupplierItemDomain;
import com.beet.backend.modules.supplier.domain.model.SupplierDomain;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service port for ingredient operations.
 */
public interface IngredientServicePort {

    /**
     * Creates a master ingredient with its first supplier item.
     * Orchestrates the Beet Math Engine (unit validation, factor calculation,
     * costing).
     *
     * @param ingredient           master ingredient data (name, baseUnitId)
     * @param supplier             supplier data (existing or quick-add)
     * @param supplierItem         supplier item data (purchaseUnitName, brandName)
     * @param conversionUnitId     the unit the user measured the purchase in (e.g.
     *                             kg)
     * @param userConversionFactor how many of conversionUnit per purchase unit
     *                             (e.g. 25)
     * @param totalPrice           total price paid for the purchase unit
     * @param ownerId              tenant owner
     */
    MasterIngredientDomain create(
            MasterIngredientDomain ingredient,
            SupplierDomain supplier,
            SupplierItemDomain supplierItem,
            UUID conversionUnitId,
            BigDecimal userConversionFactor,
            BigDecimal totalPrice,
            UUID ownerId);

    /**
     * Lists ingredients for the given owner with server-side pagination, search,
     * sorting and unit facets.
     */
    PageResponse<IngredientListResponse> list(
            UUID ownerId, int page, int size,
            String search, String sortBy, boolean sortDesc, List<String> units);

    /**
     * Returns the enriched detail of a single ingredient.
     * Returns empty if not found or the owner does not own it.
     */
    Optional<IngredientDetailResponse> findById(UUID id, UUID ownerId);
}
