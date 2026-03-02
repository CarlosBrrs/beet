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
}
