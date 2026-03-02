package com.beet.backend.modules.menu.application.handler;

import com.beet.backend.modules.menu.application.dto.*;
import com.beet.backend.modules.menu.domain.api.MenuServicePort;
import com.beet.backend.modules.menu.domain.model.MenuDomain;
import com.beet.backend.modules.menu.domain.model.SubmenuDomain;
import com.beet.backend.modules.menu.domain.spi.MenuPersistencePort;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuHandlerImpl implements MenuHandler {

    private final MenuServicePort menuServicePort;
    private final MenuPersistencePort menuPersistencePort;

    @Override
    public ApiGenericResponse<MenuResponse> createMenu(UUID restaurantId, CreateMenuRequest request) {
        MenuDomain newMenu = MenuDomain.builder()
                .restaurantId(restaurantId)
                .name(request.name())
                .description(request.description())
                .build();
        MenuDomain created = menuServicePort.createMenu(newMenu);
        return ApiGenericResponse.success(mapToMenuResponse(created));
    }

    @Override
    public ApiGenericResponse<MenuResponse> updateMenu(UUID menuId, UpdateMenuRequest request) {
        MenuDomain updateData = MenuDomain.builder()
                .id(menuId)
                .name(request.name())
                .description(request.description())
                .build();
        MenuDomain updated = menuServicePort.updateMenu(updateData);
        return ApiGenericResponse.success(mapToMenuResponse(updated));
    }

    @Override
    public ApiGenericResponse<List<MenuResponse>> findAllMenus(UUID restaurantId) {
        return ApiGenericResponse.success(menuPersistencePort.findAllWithSubmenus(restaurantId).stream()
                .map(this::mapToMenuResponse)
                .collect(Collectors.toList()));
    }

    @Override
    public ApiGenericResponse<SubmenuResponse> createSubmenu(UUID menuId, CreateSubmenuRequest request) {
        SubmenuDomain newSubmenu = SubmenuDomain.builder()
                .menuId(menuId)
                .name(request.name())
                .description(request.description())
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
                .build();
        SubmenuDomain created = menuServicePort.createSubmenu(newSubmenu);
        return ApiGenericResponse.success(mapToSubmenuResponse(created));
    }

    @Override
    public ApiGenericResponse<SubmenuResponse> updateSubmenu(UUID submenuId, UpdateSubmenuRequest request) {
        SubmenuDomain updateData = SubmenuDomain.builder()
                .id(submenuId)
                .name(request.name())
                .description(request.description())
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
                .build();
        SubmenuDomain updated = menuServicePort.updateSubmenu(updateData);
        return ApiGenericResponse.success(mapToSubmenuResponse(updated));
    }

    private MenuResponse mapToMenuResponse(MenuDomain domain) {
        List<SubmenuResponse> submenus = domain.getSubmenus().stream()
                .map(this::mapToSubmenuResponse)
                .collect(Collectors.toList());
        return new MenuResponse(
                domain.getId(),
                domain.getRestaurantId(),
                domain.getName(),
                domain.getDescription(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                submenus);
    }

    private SubmenuResponse mapToSubmenuResponse(SubmenuDomain domain) {
        return new SubmenuResponse(
                domain.getId(),
                domain.getMenuId(),
                domain.getName(),
                domain.getDescription(),
                domain.getSortOrder(),
                domain.getCreatedAt(),
                domain.getUpdatedAt());
    }
}
