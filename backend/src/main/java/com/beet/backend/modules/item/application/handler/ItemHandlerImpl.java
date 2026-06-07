package com.beet.backend.modules.item.application.handler;

import com.beet.backend.modules.item.application.dto.*;
import com.beet.backend.modules.item.domain.api.ItemServicePort;
import com.beet.backend.modules.item.domain.model.*;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
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
                .isActive(true)
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
    public ApiGenericResponse<PageResponse<ItemResponse>> getAllProducts(
            UUID restaurantId, int page, int size, String search) {
        PageResponse<ItemDomain> result = itemServicePort.getProductsPaged(restaurantId, page, size, search);
        return ApiGenericResponse.success(PageResponse.of(
                result.content().stream().map(this::toResponse).toList(),
                result.totalElements(),
                result.number(),
                result.size()));
    }

    // --------------------------------------------------------------------------
    // Products (created from within a submenu)
    // --------------------------------------------------------------------------

    @Override
    public ApiGenericResponse<ItemResponse> createProduct(UUID submenuId, UUID ownerId, CreateProductRequest request) {
        return ApiGenericResponse.success(toResponse(itemServicePort.createProduct(submenuId, toProductDomain(ownerId, request))));
    }

    @Override
    public ApiGenericResponse<ItemResponse> createProduct(UUID restaurantId, CreateProductRequest request) {
        return ApiGenericResponse.success(toResponse(itemServicePort.createProduct(toProductDomain(restaurantId, request))));
    }

    private ItemDomain toProductDomain(UUID ownerId, CreateProductRequest request) {
        boolean tracked = Boolean.TRUE.equals(request.isInventoryTracked());
        return ItemDomain.builder()
                .restaurantId(ownerId)
                .itemClass(ItemClass.PRODUCT)
                .name(request.name())
                .description(request.description())
                .isInventoryTracked(tracked)
                .salePrice(request.salePrice())
                .theoreticalCost(tracked ? null : request.userDefinedCost())
                .yieldQty(request.yieldQty())
                .yieldUnitId(request.yieldUnitId())
                .sellableUnitsPerBatch(request.sellableUnitsPerBatch())
                .recipeLines(toRecipeLineDomains(request.lines()))
                .isActive(true)
                .isAvailableAsTemplateOption(Boolean.TRUE.equals(request.isAvailableAsTemplateOption()))
                .createdBy(SecurityUtils.getAuthenticatedUserId())
                .updatedBy(SecurityUtils.getAuthenticatedUserId())
                .build();
    }

    @Override
    public ApiGenericResponse<List<ItemResponse>> getTemplateOptions(UUID restaurantId) {
        return ApiGenericResponse.success(itemServicePort.getTemplateOptions(restaurantId).stream().map(this::toResponse).toList());
    }

    @Override
    public ApiGenericResponse<ItemResponse> setProductActive(UUID restaurantId, UUID productId, boolean active) {
        return ApiGenericResponse.success(toResponse(itemServicePort.setProductActive(restaurantId, productId, active, SecurityUtils.getAuthenticatedUserId())));
    }

    @Override
    public ApiGenericResponse<ProductDependenciesResponse> getProductDependencies(UUID restaurantId, UUID productId) {
        ProductDependenciesDomain dependencies = itemServicePort.getProductDependencies(restaurantId, productId);
        return ApiGenericResponse.success(new ProductDependenciesResponse(
                dependencies.publications().stream()
                        .map(publication -> new ProductDependenciesResponse.Publication(
                                publication.menuId(), publication.menuName(),
                                publication.submenuId(), publication.submenuName()))
                        .toList(),
                dependencies.templateUsages().stream()
                        .map(usage -> new ProductDependenciesResponse.TemplateUsage(
                                usage.templateId(), usage.templateName(),
                                usage.slotId(), usage.slotName()))
                        .toList()));
    }

    // --------------------------------------------------------------------------
    // Shared update / delete / get
    // --------------------------------------------------------------------------

    @Override
    public ApiGenericResponse<ItemResponse> updateItem(UUID itemId, UpdateItemRequest request) {
        ItemDomain existing = itemServicePort.getById(itemId);
        return updateItem(existing, request, null);
    }

    @Override
    public ApiGenericResponse<ItemResponse> updateItem(UUID restaurantId, UUID itemId, UpdateItemRequest request) {
        ItemDomain existing = itemServicePort.getById(restaurantId, itemId);
        return updateItem(existing, request, restaurantId);
    }

    private ApiGenericResponse<ItemResponse> updateItem(
            ItemDomain existing, UpdateItemRequest request, UUID restaurantId) {
        UUID itemId = existing.getId();
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
                .sellableUnitsPerBatch(request.sellableUnitsPerBatch() != null
                        ? request.sellableUnitsPerBatch() : existing.getSellableUnitsPerBatch())
                .theoreticalCost(request.userDefinedCost())
                .isAvailableAsTemplateOption(request.isAvailableAsTemplateOption() != null
                        ? request.isAvailableAsTemplateOption() : existing.isAvailableAsTemplateOption())
                .recipeLines(request.lines() != null
                        ? toRecipeLineDomains(request.lines())
                        : existing.getRecipeLines())
                .updatedBy(SecurityUtils.getAuthenticatedUserId())
                .build();

        ItemDomain updated = restaurantId == null
                ? itemServicePort.updateItem(updateData)
                : itemServicePort.updateItem(restaurantId, updateData);
        return ApiGenericResponse.success(toResponse(updated));
    }

    @Override
    public ApiGenericResponse<Void> deleteItem(UUID itemId) {
        itemServicePort.deleteItem(itemId);
        return ApiGenericResponse.success(null);
    }

    @Override
    public ApiGenericResponse<Void> deleteItem(UUID restaurantId, UUID itemId) {
        itemServicePort.deleteItem(restaurantId, itemId);
        return ApiGenericResponse.success(null);
    }

    @Override
    public ApiGenericResponse<ItemResponse> getById(UUID itemId) {
        return ApiGenericResponse.success(toResponse(itemServicePort.getById(itemId)));
    }

    @Override
    public ApiGenericResponse<ItemResponse> getById(UUID restaurantId, UUID itemId) {
        return ApiGenericResponse.success(toResponse(itemServicePort.getById(restaurantId, itemId)));
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
                d.getSellableUnitsPerBatch(),
                d.getPortionSize(),
                d.getPortionUnitId(),
                d.getPortionUnitAbbreviation(),
                d.getBatchTheoreticalCost(),
                d.getSalePrice(),
                d.getTheoreticalCost(),
                d.isCostComplete(),
                d.getMissingCostIngredients(),
                d.isActive(),
                d.isAvailableAsTemplateOption(),
                d.isPublished(),
                d.isUsedAsTemplateOption(),
                d.getRecipeLines().stream().map(this::toLineResponse).collect(Collectors.toList()),
                d.getCreatedAt(),
                d.getUpdatedAt());
    }

    private RecipeLineResponse toLineResponse(RecipeLineDomain l) {
        return new RecipeLineResponse(
                l.getId(), l.getSource(), l.getMasterIngredientId(),
                l.getChildItemId(), l.getQuantity(), l.getUnitId(),
                l.getSourceName(), l.getUnitName(), l.getUnitAbbreviation(), l.getSortOrder());
    }
}
