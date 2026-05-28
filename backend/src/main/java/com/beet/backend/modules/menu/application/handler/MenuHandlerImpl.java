package com.beet.backend.modules.menu.application.handler;

import com.beet.backend.modules.item.application.dto.ItemResponse;
import com.beet.backend.modules.item.application.dto.RecipeLineResponse;
import com.beet.backend.modules.item.domain.exception.ItemNotFoundException;
import com.beet.backend.modules.item.domain.model.ItemDomain;
import com.beet.backend.modules.item.domain.model.RecipeLineDomain;
import com.beet.backend.modules.item.domain.spi.ItemPersistencePort;
import com.beet.backend.modules.menu.application.dto.*;
import com.beet.backend.modules.menu.domain.api.MenuServicePort;
import com.beet.backend.modules.menu.domain.model.MenuDomain;
import com.beet.backend.modules.menu.domain.model.SubmenuNodeDomain;
import com.beet.backend.modules.menu.domain.model.SubmenuNodeType;
import com.beet.backend.modules.menu.domain.model.SubmenuDomain;
import com.beet.backend.modules.menu.domain.spi.MenuPersistencePort;
import com.beet.backend.modules.menu.domain.spi.SubmenuNodeQueryPort;
import com.beet.backend.modules.template.application.dto.TemplateResponse;
import com.beet.backend.modules.template.domain.exception.TemplateNotFoundException;
import com.beet.backend.modules.template.domain.model.TemplateDomain;
import com.beet.backend.modules.template.domain.spi.TemplatePersistencePort;
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
        private final SubmenuNodeQueryPort submenuNodeQueryPort;
        private final ItemPersistencePort itemPersistencePort;
        private final TemplatePersistencePort templatePersistencePort;

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

        @Override
        public ApiGenericResponse<List<SubmenuNodeResponse>> getSubmenuNodes(UUID submenuId) {
                return ApiGenericResponse.success(
                                submenuNodeQueryPort.findNodesBySubmenu(submenuId).stream()
                                                .map(this::mapToSubmenuNodeResponse)
                                                .collect(Collectors.toList()));
        }

        // ----- Mappers -----------------------------------------------------------

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

        private SubmenuNodeResponse mapToSubmenuNodeResponse(SubmenuNodeDomain node) {
                ItemResponse item = null;
                TemplateResponse template = null;

                if (node.nodeType() == SubmenuNodeType.PRODUCT) {
                        ItemDomain itemDomain = itemPersistencePort.findById(node.itemId())
                                        .orElseThrow(() -> ItemNotFoundException.forId(node.itemId()));
                        itemDomain.setRecipeLines(itemPersistencePort.findRecipeLinesByParent(itemDomain.getId()));
                        item = mapToItemResponse(itemDomain);
                }

                if (node.nodeType() == SubmenuNodeType.TEMPLATE) {
                        TemplateDomain templateDomain = templatePersistencePort.findById(node.templateId())
                                        .orElseThrow(() -> TemplateNotFoundException.forId(node.templateId()));
                        template = mapToTemplateResponse(templateDomain);
                }

                return new SubmenuNodeResponse(
                                node.id(),
                                node.submenuId(),
                                node.nodeType(),
                                node.itemId(),
                                node.templateId(),
                                node.sortOrder(),
                                item,
                                template);
        }

        private ItemResponse mapToItemResponse(ItemDomain item) {
                return new ItemResponse(
                                item.getId(),
                                item.getRestaurantId(),
                                item.getItemClass(),
                                item.getName(),
                                item.getDescription(),
                                item.isInventoryTracked(),
                                item.getYieldQty(),
                                item.getYieldUnitId(),
                                item.getSalePrice(),
                                item.getTheoreticalCost(),
                                item.getRecipeLines().stream()
                                                .map(this::mapLineToResponse)
                                                .collect(Collectors.toList()),
                                item.getCreatedAt(),
                                item.getUpdatedAt());
        }

        private TemplateResponse mapToTemplateResponse(TemplateDomain template) {
                return new TemplateResponse(
                                template.getId(),
                                template.getRestaurantId(),
                                template.getName(),
                                template.getDescription(),
                                template.getBasePrice(),
                                template.getSlots().stream()
                                                .map(slot -> new TemplateResponse.SlotResponse(
                                                                slot.getId(),
                                                                slot.getName(),
                                                                slot.getMinSelection(),
                                                                slot.getMaxSelection(),
                                                                slot.getSortOrder(),
                                                                slot.getOptions().stream()
                                                                                .map(option -> new TemplateResponse.SlotOptionResponse(
                                                                                                option.getId(),
                                                                                                option.getItemId(),
                                                                                                option.getSurcharge(),
                                                                                                option.isDefault(),
                                                                                                option.getSortOrder()))
                                                                                .collect(Collectors.toList())))
                                                .collect(Collectors.toList()),
                                template.getCreatedAt(),
                                template.getUpdatedAt());
        }

        private RecipeLineResponse mapLineToResponse(RecipeLineDomain l) {
                return new RecipeLineResponse(
                                l.getId(), l.getSource(), l.getMasterIngredientId(),
                                l.getChildItemId(), l.getQuantity(), l.getUnitId(), l.getSortOrder());
        }
}
