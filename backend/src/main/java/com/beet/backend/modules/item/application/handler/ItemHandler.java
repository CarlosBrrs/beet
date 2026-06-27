package com.beet.backend.modules.item.application.handler;

import com.beet.backend.modules.item.application.dto.*;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.util.List;
import java.util.UUID;

public interface ItemHandler {

    ApiGenericResponse<ItemResponse> createPreparation(UUID restaurantId, CreatePreparationRequest request);

    ApiGenericResponse<ItemResponse> createProduct(UUID submenuId, UUID restaurantId, CreateProductRequest request);

    ApiGenericResponse<ItemResponse> updateItem(UUID itemId, UpdateItemRequest request);

    ApiGenericResponse<ItemResponse> updateItem(UUID restaurantId, UUID itemId, UpdateItemRequest request);

    ApiGenericResponse<Void> deleteItem(UUID itemId);

    ApiGenericResponse<Void> deleteItem(UUID restaurantId, UUID itemId);

    ApiGenericResponse<ItemResponse> getById(UUID itemId);

    ApiGenericResponse<ItemResponse> getById(UUID restaurantId, UUID itemId);

    ApiGenericResponse<List<ItemResponse>> getAllPreparations(UUID restaurantId);

    ApiGenericResponse<PageResponse<ItemResponse>> getAllProducts(
            UUID restaurantId, int page, int size, String search);

    ApiGenericResponse<ItemResponse> createProduct(UUID restaurantId, CreateProductRequest request);

    ApiGenericResponse<List<ItemResponse>> getTemplateOptions(UUID restaurantId);

    ApiGenericResponse<ItemResponse> setProductActive(UUID restaurantId, UUID productId, boolean active);

    ApiGenericResponse<ProductDependenciesResponse> getProductDependencies(UUID restaurantId, UUID productId);
}
