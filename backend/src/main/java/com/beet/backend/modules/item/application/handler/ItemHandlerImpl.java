package com.beet.backend.modules.item.application.handler;

import com.beet.backend.modules.item.application.dto.*;
import com.beet.backend.modules.item.domain.api.ItemServicePort;
import com.beet.backend.modules.item.domain.model.*;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemHandlerImpl implements ItemHandler {

    private final ItemServicePort itemServicePort;

    // --------------------------------------------------------------------------
    // Preparations
    // --------------------------------------------------------------------------

    @Override
    public ApiGenericResponse<ItemResponse> createPreparation(UUID restaurantId, CreatePreparationRequest request) {
        UUID currentUserId = SecurityUtils.getAuthenticatedUserId();
        ItemDomain domain = ItemDomain.builder()
                .restaurantId(restaurantId)
                .itemClass(ItemClass.PREPARATION)
                .name(request.name())
                .description(request.description())
                .isInventoryTracked(true)
                .yieldQty(request.yieldQty())
                .yieldUnitId(request.yieldUnitId())
                .recipeLines(toRecipeLineDomains(request.lines()))
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();
        return ApiGenericResponse.success(toResponse(itemServicePort.createPreparation(domain)));
    }

    @Override
    public ApiGenericResponse<List<ItemResponse>> getAllPreparations(UUID restaurantId) {
        return ApiGenericResponse.success(
                itemServicePort.getAllByRestaurantAndClass(restaurantId, ItemClass.PREPARATION)
                        .stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @Override
    public ApiGenericResponse<List<ItemResponse>> getAllProducts(UUID restaurantId) {
        return ApiGenericResponse.success(
                itemServicePort.getAllByRestaurantAndClass(restaurantId, ItemClass.SALEABLE_PRODUCT)
                        .stream().map(this::toResponse).collect(Collectors.toList()));
    }

    // --------------------------------------------------------------------------
    // Products (created from within a submenu)
    // --------------------------------------------------------------------------

    @Override
    public ApiGenericResponse<ItemResponse> createProduct(UUID submenuId, UUID ownerId, CreateProductRequest request) {
        boolean tracked = Boolean.TRUE.equals(request.isInventoryTracked());

        ItemDomain domain = ItemDomain.builder()
                .restaurantId(ownerId)
                .itemClass(ItemClass.SALEABLE_PRODUCT)
                .name(request.name())
                .description(request.description())
                .isInventoryTracked(tracked)
                .salePrice(request.salePrice())
                .theoreticalCost(tracked ? null : request.userDefinedCost())
                .yieldQty(request.yieldQty())
                .yieldUnitId(request.yieldUnitId())
                .recipeLines(toRecipeLineDomains(request.lines()))
                .createdBy(SecurityUtils.getAuthenticatedUserId())
                .updatedBy(SecurityUtils.getAuthenticatedUserId())
                .build();

        ItemDomain created = itemServicePort.createProduct(submenuId, domain);

        return ApiGenericResponse.success(toResponse(created));
    }

    // --------------------------------------------------------------------------
    // Shared update / delete / get
    // --------------------------------------------------------------------------

    @Override
    public ApiGenericResponse<ItemResponse> updateItem(UUID itemId, UpdateItemRequest request) {
        ItemDomain existing = itemServicePort.getById(itemId);

        ItemDomain updateData = ItemDomain.builder()
                .id(itemId)
                .restaurantId(existing.getRestaurantId())
                .itemClass(existing.getItemClass())
                .isInventoryTracked(existing.isInventoryTracked())
                .name(request.name() != null ? request.name() : existing.getName())
                .description(request.description() != null ? request.description() : existing.getDescription())
                .salePrice(request.salePrice() != null ? request.salePrice() : existing.getSalePrice())
                .yieldQty(request.yieldQty() != null ? request.yieldQty() : existing.getYieldQty())
                .yieldUnitId(request.yieldUnitId() != null ? request.yieldUnitId() : existing.getYieldUnitId())
                .theoreticalCost(request.userDefinedCost())
                .recipeLines(request.lines() != null ? toRecipeLineDomains(request.lines()) : List.of())
                .updatedBy(SecurityUtils.getAuthenticatedUserId())
                .build();

        return ApiGenericResponse.success(toResponse(itemServicePort.updateItem(updateData)));
    }

    @Override
    public ApiGenericResponse<Void> deleteItem(UUID itemId) {
        itemServicePort.deleteItem(itemId);
        return ApiGenericResponse.success(null);
    }

    @Override
    public ApiGenericResponse<ItemResponse> getById(UUID itemId) {
        return ApiGenericResponse.success(toResponse(itemServicePort.getById(itemId)));
    }

    // --------------------------------------------------------------------------
    // Mappers
    // --------------------------------------------------------------------------

    private List<RecipeLineDomain> toRecipeLineDomains(List<RecipeLineRequest> requests) {
        if (requests == null)
            return List.of();
        return requests.stream().map(r -> {
            RecipeLineSource source = r.source() != null ? r.source() : RecipeLineSource.INGREDIENT;
            return RecipeLineDomain.builder()
                .source(source)
                .masterIngredientId(r.masterIngredientId())
                .childItemId(r.childItemId())
                .quantity(r.quantity())
                .unitId(r.unitId())
                .build();
        }).collect(Collectors.toList());
    }

    private ItemResponse toResponse(ItemDomain d) {
        return new ItemResponse(
                d.getId(),
                d.getRestaurantId(),
                d.getItemClass(),
                d.getName(),
                d.getDescription(),
                d.isInventoryTracked(),
                d.getYieldQty(),
                d.getYieldUnitId(),
                d.getSalePrice(),
                d.getTheoreticalCost(),
                d.getRecipeLines().stream().map(this::toLineResponse).collect(Collectors.toList()),
                d.getCreatedAt(),
                d.getUpdatedAt());
    }

    private RecipeLineResponse toLineResponse(RecipeLineDomain l) {
        return new RecipeLineResponse(
                l.getId(), l.getSource(), l.getMasterIngredientId(),
                l.getChildItemId(), l.getQuantity(), l.getUnitId(), l.getSortOrder());
    }
}
