package com.beet.backend.modules.item.application.handler;

import com.beet.backend.modules.item.application.dto.*;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;

import java.util.List;
import java.util.UUID;

public interface ItemHandler {

    ApiGenericResponse<ItemResponse> createPreparation(UUID restaurantId, CreatePreparationRequest request);

    ApiGenericResponse<ItemResponse> createProduct(UUID submenuId, UUID restaurantId, CreateProductRequest request);

    ApiGenericResponse<ItemResponse> updateItem(UUID itemId, UpdateItemRequest request);

    ApiGenericResponse<Void> deleteItem(UUID itemId);

    ApiGenericResponse<ItemResponse> getById(UUID itemId);

    ApiGenericResponse<List<ItemResponse>> getAllPreparations(UUID restaurantId);

    ApiGenericResponse<List<ItemResponse>> getAllProducts(UUID restaurantId);
}
