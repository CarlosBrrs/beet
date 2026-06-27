package com.beet.backend.modules.menu.application.handler;

import com.beet.backend.modules.menu.application.dto.*;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;

import java.util.List;
import java.util.UUID;

public interface MenuHandler {
    ApiGenericResponse<MenuResponse> createMenu(UUID restaurantId, CreateMenuRequest request);

    ApiGenericResponse<MenuResponse> updateMenu(UUID menuId, UpdateMenuRequest request);

    ApiGenericResponse<List<MenuResponse>> findAllMenus(UUID restaurantId);

    ApiGenericResponse<SubmenuResponse> createSubmenu(UUID menuId, CreateSubmenuRequest request);

    ApiGenericResponse<SubmenuResponse> updateSubmenu(UUID submenuId, UpdateSubmenuRequest request);

    ApiGenericResponse<List<SubmenuNodeResponse>> getSubmenuNodes(UUID restaurantId, UUID menuId, UUID submenuId);

    ApiGenericResponse<SubmenuNodeResponse> publishSubmenuNode(
            UUID restaurantId, UUID menuId, UUID submenuId, PublishSubmenuNodeRequest request);

    ApiGenericResponse<Void> deleteSubmenuNode(UUID restaurantId, UUID menuId, UUID submenuId, UUID nodeId);

    void assertMenuPath(UUID restaurantId, UUID menuId);

    void assertSubmenuPath(UUID restaurantId, UUID menuId, UUID submenuId);
}
